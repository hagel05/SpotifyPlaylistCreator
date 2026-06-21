package org.hagelbrand.config;

import org.hagelbrand.data.AppUser;
import org.hagelbrand.service.AppUserService;
import org.hagelbrand.service.DuplicateEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock AppUserService appUserService;

    @InjectMocks OAuth2LoginSuccessHandler handler;

    private MockHttpSession session;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private OAuth2AuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        request = new MockHttpServletRequest();
        request.setSession(session);
        response = new MockHttpServletResponse();

        OAuth2User oAuth2User = mock(OAuth2User.class);
        authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("spotify");
        when(oAuth2User.getName()).thenReturn("spotify_user_123");
        when(oAuth2User.<String>getAttribute("display_name")).thenReturn("Test User");
        when(oAuth2User.<String>getAttribute("email")).thenReturn("test@example.com");
    }

    // ── Normal login ──────────────────────────────────────────────────────────

    @Test
    void successfulLogin_storesAppUserIdInSession() throws Exception {
        UUID appUserId = UUID.randomUUID();
        AppUser appUser = new AppUser(appUserId, "Test User", List.of(), null);
        when(appUserService.findOrCreateByProvider("spotify", "spotify_user_123", "test@example.com", "Test User"))
                .thenReturn(appUser);

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID))
                .isEqualTo(appUserId.toString());
    }

    @Test
    void loginWithDuplicateEmail_redirectsWithEmailConflictError() throws Exception {
        when(appUserService.findOrCreateByProvider(any(), any(), any(), any()))
                .thenThrow(new DuplicateEmailException("spotify"));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).contains("error=email_conflict");
        assertThat(response.getRedirectedUrl()).contains("existingProvider=spotify");
    }

    @Test
    void loginWithDuplicateEmail_doesNotSetSessionUserId() throws Exception {
        when(appUserService.findOrCreateByProvider(any(), any(), any(), any()))
                .thenThrow(new DuplicateEmailException("spotify"));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID)).isNull();
    }

    // ── Provider link flow ────────────────────────────────────────────────────

    @Test
    void linkIntent_callsLinkProviderWithExistingUserId() throws Exception {
        UUID existingUserId = UUID.randomUUID();
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, existingUserId.toString());
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_LINK_PROVIDER_INTENT, true);

        AppUser appUser = new AppUser(existingUserId, "Test User", List.of(), null);
        when(appUserService.linkProvider(eq(existingUserId), eq("spotify"), eq("spotify_user_123"), eq("test@example.com")))
                .thenReturn(appUser);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(appUserService).linkProvider(existingUserId, "spotify", "spotify_user_123", "test@example.com");
        verify(appUserService, never()).findOrCreateByProvider(any(), any(), any(), any());
    }

    @Test
    void linkIntent_clearsIntentFromSession() throws Exception {
        UUID existingUserId = UUID.randomUUID();
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, existingUserId.toString());
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_LINK_PROVIDER_INTENT, true);

        when(appUserService.linkProvider(any(), any(), any(), any()))
                .thenReturn(new AppUser(existingUserId, "Test User", List.of(), null));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(session.getAttribute(OAuth2LoginSuccessHandler.SESSION_LINK_PROVIDER_INTENT)).isNull();
    }

    @Test
    void linkIntent_withExpiredSession_redirectsWithError() throws Exception {
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_LINK_PROVIDER_INTENT, true);
        // SESSION_APP_USER_ID intentionally absent

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).contains("error=link_session_expired");
        verify(appUserService, never()).linkProvider(any(), any(), any(), any());
    }

    @Test
    void linkIntent_withProviderAlreadyOnDifferentUser_redirectsWithError() throws Exception {
        UUID existingUserId = UUID.randomUUID();
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, existingUserId.toString());
        session.setAttribute(OAuth2LoginSuccessHandler.SESSION_LINK_PROVIDER_INTENT, true);

        when(appUserService.linkProvider(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("This spotify account is already linked to a different user"));

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).contains("error=link_failed");
    }
}
