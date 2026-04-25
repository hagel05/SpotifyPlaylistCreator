package org.hagelbrand.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SpotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void checkAuthReturnsAuthenticatedFalseWhenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/api/auth/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void checkAuthReturnsAuthenticatedTrueWhenLoggedIn() throws Exception {
        mockMvc.perform(
                get("/api/auth/check")
                        .with(oauth2Login().clientRegistration(
                                ClientRegistration.withRegistrationId("spotify")
                                        .clientId("client-id")
                                        .clientSecret("client-secret")
                                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .redirectUri("http://localhost/login/oauth2/code/spotify")
                                        .authorizationUri("https://accounts.spotify.com/authorize")
                                        .tokenUri("https://accounts.spotify.com/api/token")
                                        .scope("playlist-modify-public")
                                        .build()
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void checkAuthIncludesPrincipalWhenAuthenticated() throws Exception {
        mockMvc.perform(
                get("/api/auth/check")
                        .with(oauth2Login().clientRegistration(
                                ClientRegistration.withRegistrationId("spotify")
                                        .clientId("client-id")
                                        .clientSecret("client-secret")
                                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .redirectUri("http://localhost/login/oauth2/code/spotify")
                                        .authorizationUri("https://accounts.spotify.com/authorize")
                                        .tokenUri("https://accounts.spotify.com/api/token")
                                        .scope("playlist-modify-public")
                                        .build()
                        ))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.principal").exists());
    }

    @Test
    void getSpotifyMeRequiresOAuth2Authentication() throws Exception {
        mockMvc.perform(get("/api/spotify/me"))
                .andExpect(status().is3xxRedirection());
    }
}
