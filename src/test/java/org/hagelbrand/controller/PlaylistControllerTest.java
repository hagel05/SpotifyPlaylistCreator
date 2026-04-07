package org.hagelbrand.controller;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.hagelbrand.service.spotify.SpotifyPlaylistOrchestrator;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArtistSetlistPredictorService artistSetlistPredictorService;

    @MockBean
    private SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator;

    @Test
    void createsSpotifyPlaylistForArtist() throws Exception {
        String artist = "Green Day";
        int limit = 10;
        String playlistId = "playlist123";

        List<TrackCount> tracks =
                List.of(
                        new TrackCount("When I Come Around", 4),
        new TrackCount("Basket Case", 5)
                );

        when(artistSetlistPredictorService.getTopTracksForArtist(artist, limit))
                .thenReturn(tracks);

        when(spotifyPlaylistOrchestrator.buildPlaylist(
                any(OAuth2AuthorizedClient.class),
                eq(artist),
                eq(tracks)
        )).thenReturn(playlistId);

        mockMvc.perform(
                        get("/api/playlist/{artist}/spotify-playlist", artist)
                                .param("limit", String.valueOf(limit))
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
                .andExpect(jsonPath("$.playlistId").value(playlistId))
                .andExpect(jsonPath("$.url")
                        .value("https://open.spotify.com/playlist/" + playlistId));
    }
}
