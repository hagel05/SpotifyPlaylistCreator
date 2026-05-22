package org.hagelbrand.controller;

import org.hagelbrand.data.ArtistSuggestion;
import org.hagelbrand.service.spotify.ArtistSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ArtistSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArtistSearchService artistSearchService;

    private static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor spotifyLogin() {
        return oauth2Login().clientRegistration(
                ClientRegistration.withRegistrationId("spotify")
                        .clientId("client-id")
                        .clientSecret("client-secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://localhost/login/oauth2/code/spotify")
                        .authorizationUri("https://accounts.spotify.com/authorize")
                        .tokenUri("https://accounts.spotify.com/api/token")
                        .scope("playlist-modify-public")
                        .build()
        );
    }

    @Test
    void returnsArtistSuggestionsForValidQuery() throws Exception {
        List<ArtistSuggestion> suggestions = List.of(
                new ArtistSuggestion("1", "The Beatles", "https://i.scdn.co/image/beatles.jpg"),
                new ArtistSuggestion("2", "Beatles Revival", null)
        );

        when(artistSearchService.searchArtists(eq("Beatles"), any(OAuth2AuthorizedClient.class)))
                .thenReturn(suggestions);

        mockMvc.perform(get("/api/artists/search")
                        .param("q", "Beatles")
                        .with(spotifyLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("The Beatles"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://i.scdn.co/image/beatles.jpg"))
                .andExpect(jsonPath("$[1].name").value("Beatles Revival"))
                .andExpect(jsonPath("$[1].imageUrl").isEmpty());

        verify(artistSearchService).searchArtists(eq("Beatles"), any(OAuth2AuthorizedClient.class));
    }

    @Test
    void returnsEmptyListWhenQueryTooShort() throws Exception {
        mockMvc.perform(get("/api/artists/search")
                        .param("q", "a")
                        .with(spotifyLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returnsEmptyListForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/artists/search")
                        .param("q", "   ")
                        .with(spotifyLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/artists/search")
                        .param("q", "Beatles"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void returnsEmptyListWhenServiceReturnsEmpty() throws Exception {
        when(artistSearchService.searchArtists(eq("xyz"), any(OAuth2AuthorizedClient.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/artists/search")
                        .param("q", "xyz")
                        .with(spotifyLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
