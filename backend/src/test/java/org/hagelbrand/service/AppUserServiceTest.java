package org.hagelbrand.service;

import org.hagelbrand.data.AppUser;
import org.hagelbrand.entity.AppUserEntity;
import org.hagelbrand.entity.ProviderAccountEntity;
import org.hagelbrand.repository.AppUserRepository;
import org.hagelbrand.repository.PlaylistRepository;
import org.hagelbrand.repository.PlaylistVersionRepository;
import org.hagelbrand.repository.ProviderAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock AppUserRepository appUserRepository;
    @Mock ProviderAccountRepository providerAccountRepository;
    @Mock PlaylistRepository playlistRepository;
    @Mock PlaylistVersionRepository playlistVersionRepository;

    @InjectMocks AppUserService appUserService;

    private static final UUID EXISTING_USER_ID = UUID.randomUUID();

    private AppUserEntity existingUser;
    private ProviderAccountEntity spotifyAccount;

    @BeforeEach
    void setUp() throws Exception {
        existingUser = new AppUserEntity();
        existingUser.setDisplayName("Test User");
        setEntityId(existingUser, EXISTING_USER_ID);

        spotifyAccount = new ProviderAccountEntity();
        spotifyAccount.setProvider("spotify");
        spotifyAccount.setProviderUserId("spotify_user_123");
        spotifyAccount.setProviderEmail("test@example.com");
        spotifyAccount.setAppUser(existingUser);
        existingUser.getProviderAccounts().add(spotifyAccount);
    }

    private static void setEntityId(AppUserEntity entity, UUID id) throws Exception {
        Field field = AppUserEntity.class.getDeclaredField("appUserId");
        field.setAccessible(true);
        field.set(entity, id);
    }

    // ── findOrCreateByProvider ────────────────────────────────────────────────

    @Test
    void findOrCreate_returnsExistingUser_whenProviderMatches() {
        when(appUserRepository.findByProvider("spotify", "spotify_user_123"))
                .thenReturn(Optional.of(existingUser));

        AppUser result = appUserService.findOrCreateByProvider(
                "spotify", "spotify_user_123", "test@example.com", "Test User");

        assertThat(result.displayName()).isEqualTo("Test User");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void findOrCreate_createsNewUser_whenNoProviderOrEmailMatch() {
        when(appUserRepository.findByProvider("deezer", "deezer_user_456"))
                .thenReturn(Optional.empty());
        when(providerAccountRepository.findFirstByProviderEmail("new@example.com"))
                .thenReturn(Optional.empty());

        AppUserEntity saved = new AppUserEntity();
        saved.setDisplayName("New User");
        ProviderAccountEntity newAccount = new ProviderAccountEntity();
        newAccount.setProvider("deezer");
        newAccount.setProviderUserId("deezer_user_456");
        newAccount.setProviderEmail("new@example.com");
        newAccount.setAppUser(saved);
        saved.getProviderAccounts().add(newAccount);
        when(appUserRepository.save(any())).thenReturn(saved);

        AppUser result = appUserService.findOrCreateByProvider(
                "deezer", "deezer_user_456", "new@example.com", "New User");

        assertThat(result.displayName()).isEqualTo("New User");
        verify(appUserRepository).save(any());
    }

    @Test
    void findOrCreate_throwsDuplicateEmailException_whenEmailAlreadyLinked() {
        when(appUserRepository.findByProvider("deezer", "deezer_user_456"))
                .thenReturn(Optional.empty());
        when(providerAccountRepository.findFirstByProviderEmail("test@example.com"))
                .thenReturn(Optional.of(spotifyAccount));

        assertThatThrownBy(() -> appUserService.findOrCreateByProvider(
                "deezer", "deezer_user_456", "test@example.com", "Test User"))
                .isInstanceOf(DuplicateEmailException.class)
                .extracting(e -> ((DuplicateEmailException) e).getExistingProvider())
                .isEqualTo("spotify");

        verify(appUserRepository, never()).save(any());
    }

    @Test
    void findOrCreate_skipsEmailCheck_whenEmailIsNull() {
        when(appUserRepository.findByProvider("deezer", "deezer_user_456"))
                .thenReturn(Optional.empty());

        AppUserEntity saved = new AppUserEntity();
        saved.setDisplayName("No Email User");
        ProviderAccountEntity account = new ProviderAccountEntity();
        account.setProvider("deezer");
        account.setAppUser(saved);
        saved.getProviderAccounts().add(account);
        when(appUserRepository.save(any())).thenReturn(saved);

        appUserService.findOrCreateByProvider("deezer", "deezer_user_456", null, "No Email User");

        verify(providerAccountRepository, never()).findFirstByProviderEmail(any());
        verify(appUserRepository).save(any());
    }

    // ── linkProvider ──────────────────────────────────────────────────────────

    @Test
    void linkProvider_addsNewProviderToExistingUser() {
        when(appUserRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(providerAccountRepository.findByProviderAndProviderUserId("deezer", "deezer_456"))
                .thenReturn(Optional.empty());

        AppUserEntity saved = new AppUserEntity();
        saved.setDisplayName("Test User");
        ProviderAccountEntity deezerAccount = new ProviderAccountEntity();
        deezerAccount.setProvider("deezer");
        deezerAccount.setAppUser(saved);
        saved.getProviderAccounts().add(spotifyAccount);
        saved.getProviderAccounts().add(deezerAccount);
        when(appUserRepository.save(existingUser)).thenReturn(saved);

        AppUser result = appUserService.linkProvider(EXISTING_USER_ID, "deezer", "deezer_456", "test@example.com");

        assertThat(result.linkedProviders()).hasSize(2);
        verify(appUserRepository).save(existingUser);
    }

    @Test
    void linkProvider_isIdempotent_whenProviderAlreadyLinked() {
        when(appUserRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(providerAccountRepository.findByProviderAndProviderUserId("spotify", "spotify_user_123"))
                .thenReturn(Optional.of(spotifyAccount));

        AppUser result = appUserService.linkProvider(EXISTING_USER_ID, "spotify", "spotify_user_123", "test@example.com");

        assertThat(result.linkedProviders()).hasSize(1);
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void linkProvider_throws_whenProviderLinkedToDifferentUser() throws Exception {
        AppUserEntity otherUser = new AppUserEntity();
        setEntityId(otherUser, UUID.randomUUID()); // different ID from EXISTING_USER_ID

        ProviderAccountEntity deezerOnOtherUser = new ProviderAccountEntity();
        deezerOnOtherUser.setProvider("deezer");
        deezerOnOtherUser.setProviderUserId("deezer_456");
        deezerOnOtherUser.setAppUser(otherUser);

        when(appUserRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(providerAccountRepository.findByProviderAndProviderUserId("deezer", "deezer_456"))
                .thenReturn(Optional.of(deezerOnOtherUser));

        assertThatThrownBy(() -> appUserService.linkProvider(EXISTING_USER_ID, "deezer", "deezer_456", "other@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already linked to a different user");

        verify(appUserRepository, never()).save(any());
    }

    // ── unlinkProvider ────────────────────────────────────────────────────────

    @Test
    void unlinkProvider_throws_whenOnlyOneProviderLinked() {
        UUID userId = UUID.randomUUID();
        when(providerAccountRepository.findAllByAppUser_AppUserId(userId))
                .thenReturn(java.util.List.of(spotifyAccount));

        assertThatThrownBy(() -> appUserService.unlinkProvider(userId, "spotify"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot unlink the only linked provider");
    }
}
