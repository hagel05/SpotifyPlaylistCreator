package org.hagelbrand.controller;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hagelbrand.data.TrackCount;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.hagelbrand.service.spotify.SpotifyPlaylistOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional/End-to-End integration tests for the complete playlist creation workflow.
 * Tests the full flow from controller → services → playlist creation with realistic scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlaylistControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArtistSetlistPredictorService artistSetlistPredictorService;

    @MockBean
    private SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator;

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void createsPlaylistSuccessfullyWithMultipleTracks() throws Exception {
        // Setup
        String artist = "Green Day";
        int limit = 5;
        String playlistId = "playlist-greenday-123";

        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 8),
                new TrackCount("Basket Case", 7),
                new TrackCount("American Idiot", 6),
                new TrackCount("Holiday", 5),
                new TrackCount("Boulevard of Broken Dreams", 4)
        );

        when(artistSetlistPredictorService.getTopTracksForArtist(artist, limit))
                .thenReturn(tracks);

        when(spotifyPlaylistOrchestrator.buildPlaylist(
                any(OAuth2AuthorizedClient.class),
                eq(artist),
                any()
        )).thenReturn(playlistId);

        // Execute
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
                .andExpect(jsonPath("$.url").value("https://open.spotify.com/playlist/" + playlistId));
    }

    @Test
    void usesDefaultLimitWhenNotProvided() throws Exception {
        String artist = "The Beatles";
        String playlistId = "playlist-beatles-456";
        List<TrackCount> defaultLimitTracks = List.of(
                new TrackCount("Hey Jude", 10),
                new TrackCount("Let It Be", 9)
        );

        when(artistSetlistPredictorService.getTopTracksForArtist(artist, 20))
                .thenReturn(defaultLimitTracks);

        when(spotifyPlaylistOrchestrator.buildPlaylist(
                any(OAuth2AuthorizedClient.class),
                eq(artist),
                any()
        )).thenReturn(playlistId);

        mockMvc.perform(
                        get("/api/playlist/{artist}/spotify-playlist", artist)
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
                .andExpect(jsonPath("$.playlistId").value(playlistId));
    }

    @Test
    void sortTracksBeforeCreatingPlaylist() throws Exception {
        String artist = "Foo Fighters";
        int limit = 3;
        String playlistId = "playlist-foo-789";

        // Unsorted tracks (not in order of play count)
        List<TrackCount> unsortedTracks = List.of(
                new TrackCount("Everlong", 5),
                new TrackCount("The Pretender", 8),
                new TrackCount("Best of You", 6)
        );

        // Expected: sorted by play count (descending)
        List<TrackCount> expectedSortedTracks = List.of(
                new TrackCount("The Pretender", 8),
                new TrackCount("Best of You", 6),
                new TrackCount("Everlong", 5)
        );

        when(artistSetlistPredictorService.getTopTracksForArtist(artist, limit))
                .thenReturn(unsortedTracks);

        when(spotifyPlaylistOrchestrator.buildPlaylist(
                any(OAuth2AuthorizedClient.class),
                eq(artist),
                any()
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
                .andExpect(jsonPath("$.playlistId").value(playlistId));
    }

    @Test
    void handlesArtistWithSingleTrack() throws Exception {
        String artist = "Solo Artist";
        String playlistId = "playlist-solo-101";

        List<TrackCount> singleTrack = List.of(
                new TrackCount("Only Song", 1)
        );

        when(artistSetlistPredictorService.getTopTracksForArtist(artist, 20))
                .thenReturn(singleTrack);

        when(spotifyPlaylistOrchestrator.buildPlaylist(
                any(OAuth2AuthorizedClient.class),
                eq(artist),
                any()
        )).thenReturn(playlistId);

        mockMvc.perform(
                        get("/api/playlist/{artist}/spotify-playlist", artist)
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
                .andExpect(jsonPath("$.playlistId").value(playlistId));
    }

    @Test
    void returnsProperUrlFormatInResponse() throws Exception {
        String artist = "Queen";
        String playlistId = "queen-bohemian-rhapsody";

        when(artistSetlistPredictorService.getTopTracksForArtist(artist, 20))
                .thenReturn(List.of(new TrackCount("Bohemian Rhapsody", 15)));

        when(spotifyPlaylistOrchestrator.buildPlaylist(
                any(OAuth2AuthorizedClient.class),
                eq(artist),
                any()
        )).thenReturn(playlistId);

        mockMvc.perform(
                        get("/api/playlist/{artist}/spotify-playlist", artist)
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
                .andExpect(jsonPath("$.url")
                        .value("https://open.spotify.com/playlist/" + playlistId));
    }
}

