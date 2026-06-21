package org.hagelbrand.service;

import org.hagelbrand.data.AppUser;
import org.hagelbrand.data.PlaylistMetadata;
import org.hagelbrand.data.ProviderAccount;
import org.hagelbrand.entity.AppUserEntity;
import org.hagelbrand.entity.PlaylistEntity;
import org.hagelbrand.entity.PlaylistVersionEntity;
import org.hagelbrand.entity.ProviderAccountEntity;
import org.hagelbrand.repository.AppUserRepository;
import org.hagelbrand.repository.PlaylistRepository;
import org.hagelbrand.repository.PlaylistVersionRepository;
import org.hagelbrand.repository.ProviderAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class AppUserService {

    private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

    private final AppUserRepository appUserRepository;
    private final ProviderAccountRepository providerAccountRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistVersionRepository playlistVersionRepository;

    public AppUserService(
            AppUserRepository appUserRepository,
            ProviderAccountRepository providerAccountRepository,
            PlaylistRepository playlistRepository,
            PlaylistVersionRepository playlistVersionRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.providerAccountRepository = providerAccountRepository;
        this.playlistRepository = playlistRepository;
        this.playlistVersionRepository = playlistVersionRepository;
    }

    // ── User onboarding ───────────────────────────────────────────────────────

    /**
     * Finds an existing app user by provider + provider user ID, or creates a new
     * one. Called on every successful OAuth2 login.
     */
    @Transactional
    public AppUser findOrCreateByProvider(String provider, String providerUserId, String providerEmail, String displayName) {
        return appUserRepository.findByProvider(provider, providerUserId)
                .map(entity -> {
                    log.info("Returning user {} logged in via {}", entity.getAppUserId(), provider);
                    return toDto(entity);
                })
                .orElseGet(() -> {
                    if (providerEmail != null) {
                        providerAccountRepository.findFirstByProviderEmail(providerEmail)
                                .ifPresent(conflict -> {
                                    throw new DuplicateEmailException(conflict.getProvider());
                                });
                    }
                    log.info("First login via {} for providerUserId={} — creating app user", provider, providerUserId);
                    AppUserEntity user = new AppUserEntity();
                    user.setDisplayName(displayName);

                    ProviderAccountEntity providerAccount = new ProviderAccountEntity();
                    providerAccount.setProvider(provider);
                    providerAccount.setProviderUserId(providerUserId);
                    providerAccount.setProviderEmail(providerEmail);
                    providerAccount.setAppUser(user);
                    user.getProviderAccounts().add(providerAccount);

                    AppUserEntity saved = appUserRepository.save(user);
                    return toDto(saved);
                });
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AppUser getUser(UUID appUserId) {
        return appUserRepository.findById(appUserId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + appUserId));
    }

    @Transactional
    public AppUser updateDisplayName(UUID appUserId, String displayName) {
        AppUserEntity entity = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + appUserId));
        entity.setDisplayName(displayName);
        return toDto(appUserRepository.save(entity));
    }

    // ── Provider linking ──────────────────────────────────────────────────────

    @Transactional
    public AppUser linkProvider(UUID appUserId, String provider, String providerUserId, String providerEmail) {
        AppUserEntity user = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + appUserId));

        providerAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .ifPresent(existing -> {
                    if (!Objects.equals(existing.getAppUser().getAppUserId(), appUserId)) {
                        throw new IllegalStateException(
                                "This " + provider + " account is already linked to a different user");
                    }
                });

        boolean alreadyLinked = user.getProviderAccounts().stream()
                .anyMatch(a -> a.getProvider().equals(provider));
        if (alreadyLinked) {
            return toDto(user);
        }

        ProviderAccountEntity newAccount = new ProviderAccountEntity();
        newAccount.setProvider(provider);
        newAccount.setProviderUserId(providerUserId);
        newAccount.setProviderEmail(providerEmail);
        newAccount.setAppUser(user);
        user.getProviderAccounts().add(newAccount);

        log.info("Linked provider {} to user {}", provider, appUserId);
        return toDto(appUserRepository.save(user));
    }

    @Transactional
    public void unlinkProvider(UUID appUserId, String provider) {
        List<ProviderAccountEntity> accounts =
                providerAccountRepository.findAllByAppUser_AppUserId(appUserId);

        if (accounts.size() <= 1) {
            throw new IllegalStateException("Cannot unlink the only linked provider");
        }

        accounts.stream()
                .filter(a -> a.getProvider().equals(provider))
                .findFirst()
                .ifPresent(providerAccountRepository::delete);

        log.info("Unlinked provider {} from user {}", provider, appUserId);
    }

    // ── Playlists ─────────────────────────────────────────────────────────────

    @Transactional
    public PlaylistMetadata savePlaylist(
            UUID appUserId,
            String provider,
            String providerPlaylistId,
            String customName,
            String description,
            String sourceType,
            List<String> trackIds
    ) {
        AppUserEntity user = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + appUserId));

        PlaylistEntity playlist = new PlaylistEntity();
        playlist.setAppUser(user);
        playlist.setProvider(provider);
        playlist.setProviderPlaylistId(providerPlaylistId);
        playlist.setCustomName(customName);
        playlist.setDescription(description);
        playlist.setSourceType(sourceType);
        playlist.setTrackIds(trackIds);

        PlaylistEntity saved = playlistRepository.save(playlist);
        recordVersion(saved, "CREATED");
        log.info("Saved playlist {} for user {}", saved.getPlaylistId(), appUserId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PlaylistMetadata> getPlaylists(UUID appUserId) {
        return playlistRepository.findAllByAppUser_AppUserIdOrderByCreatedAtDesc(appUserId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PlaylistMetadata getPlaylist(UUID appUserId, UUID playlistId) {
        PlaylistEntity entity = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        if (!entity.getAppUser().getAppUserId().equals(appUserId)) {
            throw new IllegalArgumentException("Playlist not found: " + playlistId);
        }
        return toDto(entity);
    }

    @Transactional
    public PlaylistMetadata updatePlaylist(UUID appUserId, UUID playlistId, String customName, String description) {
        PlaylistEntity entity = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        if (!entity.getAppUser().getAppUserId().equals(appUserId)) {
            throw new IllegalArgumentException("Playlist not found: " + playlistId);
        }
        if (customName != null) entity.setCustomName(customName);
        if (description != null) entity.setDescription(description);
        PlaylistEntity saved = playlistRepository.save(entity);
        recordVersion(saved, "UPDATED");
        return toDto(saved);
    }

    @Transactional
    public void deletePlaylist(UUID appUserId, UUID playlistId) {
        PlaylistEntity entity = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found: " + playlistId));
        if (!entity.getAppUser().getAppUserId().equals(appUserId)) {
            throw new IllegalArgumentException("Playlist not found: " + playlistId);
        }
        playlistRepository.delete(entity);
        log.info("Deleted playlist {} for user {}", playlistId, appUserId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void recordVersion(PlaylistEntity playlist, String changeType) {
        PlaylistVersionEntity version = new PlaylistVersionEntity();
        version.setPlaylist(playlist);
        version.setTrackIds(playlist.getTrackIds());
        version.setChangeType(changeType);
        playlistVersionRepository.save(version);
    }

    private AppUser toDto(AppUserEntity entity) {
        List<ProviderAccount> providers = entity.getProviderAccounts().stream()
                .map(p -> new ProviderAccount(
                        p.getProviderAccountId(),
                        p.getProvider(),
                        p.getProviderUserId(),
                        p.getProviderEmail(),
                        p.getLinkedAt()
                )).toList();
        return new AppUser(entity.getAppUserId(), entity.getDisplayName(), providers, entity.getCreatedAt());
    }

    private PlaylistMetadata toDto(PlaylistEntity entity) {
        return new PlaylistMetadata(
                entity.getPlaylistId(),
                entity.getProvider(),
                entity.getProviderPlaylistId(),
                entity.getCustomName(),
                entity.getDescription(),
                entity.getSourceType(),
                entity.getTrackIds(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
