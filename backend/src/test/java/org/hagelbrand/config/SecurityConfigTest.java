package org.hagelbrand.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/test"))
                .andExpect(status().isNotFound());
        // Not 401 / 302 → security allowed it through
    }

    @Test
    void loginEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void oauth2AuthorizationEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/spotify"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void oauth2CodeEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login/oauth2/code/spotify"))
                .andExpect(status().is3xxRedirection());
        // Redirect is expected since we're not actually completing OAuth2 flow
    }

    @Test
    void authCheckEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/check"))
                .andExpect(status().isOk());
    }

    @Test
    void nonApiEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/private"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void authenticatedUserCanAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/private"))
                .andExpect(status().isNotFound());
    }

    @Test
    void spotifyMeEndpointRequiresOAuth2Authentication() throws Exception {
        mockMvc.perform(get("/api/spotify/me"))
                .andExpect(status().is3xxRedirection());
        // Requires OAuth2, so redirects to login
    }
}
