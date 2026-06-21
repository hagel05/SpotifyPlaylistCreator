package org.hagelbrand.controller;

import org.hagelbrand.config.OAuth2LoginSuccessHandler;
import org.hagelbrand.data.AppUser;
import org.hagelbrand.data.ProviderAccount;
import org.hagelbrand.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AppUserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AppUserService appUserService;

    private static final UUID USER_ID = UUID.randomUUID();

    // ── GET /api/users/me ─────────────────────────────────────────────────────

    @Test
    void getMe_withValidSession_returnsUser() throws Exception {
        AppUser appUser = new AppUser(USER_ID, "Test User", List.of(), Instant.now());
        when(appUserService.getUser(USER_ID)).thenReturn(appUser);

        mockMvc.perform(get("/api/users/me")
                        .sessionAttr(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }

    @Test
    void getMe_withNoSession_throwsIllegalState() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/users/me")).andReturn())
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    // ── GET /api/users/me/providers ───────────────────────────────────────────

    @Test
    void getLinkedProviders_returnsProviderList() throws Exception {
        ProviderAccount spotifyAccount = new ProviderAccount(
                UUID.randomUUID(), "spotify", "spotify_user_123", "test@example.com", Instant.now());
        AppUser appUser = new AppUser(USER_ID, "Test User", List.of(spotifyAccount), Instant.now());
        when(appUserService.getUser(USER_ID)).thenReturn(appUser);

        mockMvc.perform(get("/api/users/me/providers")
                        .sessionAttr(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("spotify"));
    }

    // ── GET /api/users/me/providers/link/{provider} ───────────────────────────

    @Test
    void initiateProviderLink_authenticated_setsIntentAndRedirects() throws Exception {
        mockMvc.perform(get("/api/users/me/providers/link/deezer")
                        .sessionAttr(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, USER_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/deezer"));
    }

    @Test
    void initiateProviderLink_notAuthenticated_throwsIllegalState() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/users/me/providers/link/deezer")).andReturn())
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    // ── PUT /api/users/me ─────────────────────────────────────────────────────

    @Test
    void updateMe_withValidSession_updatesDisplayName() throws Exception {
        AppUser updated = new AppUser(USER_ID, "New Name", List.of(), Instant.now());
        when(appUserService.updateDisplayName(USER_ID, "New Name")).thenReturn(updated);

        mockMvc.perform(put("/api/users/me")
                        .sessionAttr(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, USER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"));
    }

    // ── DELETE /api/users/me/providers/{provider} ─────────────────────────────

    @Test
    void unlinkProvider_withValidSession_returns200() throws Exception {
        mockMvc.perform(delete("/api/users/me/providers/spotify")
                        .sessionAttr(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID, USER_ID.toString()))
                .andExpect(status().isOk());
    }
}
