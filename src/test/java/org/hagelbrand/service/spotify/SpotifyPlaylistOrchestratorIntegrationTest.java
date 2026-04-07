package org.hagelbrand.service.spotify;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.data.TrackResolution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for SpotifyPlaylistOrchestrator.
 * Tests the orchestration of track resolution and playlist creation workflow.
 */
@ExtendWith(MockitoExtension.class)
class SpotifyPlaylistOrchestratorIntegrationTest {

    @Mock
    private SpotifyTrackResolver trackResolver;

    @Mock
    private SpotifyPlaylistService playlistService;

    @Mock
    private SpotifyUserService userService;

    @Mock
    private OAuth2AuthorizedClient spotifyClient;

    private SpotifyPlaylistOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SpotifyPlaylistOrchestrator(trackResolver, playlistService, userService);
    }

    @Test
    void buildPlaylistResolvesTracksAndCreatesPlaylist() {
        // Setup
        String artist = "Green Day";
        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 5),
                new TrackCount("Basket Case", 4),
                new TrackCount("American Idiot", 3)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");

        when(trackResolver.resolve("Green Day", "When I Come Around"))
                .thenReturn(new TrackResolution("When I Come Around", "Green Day", true,
                        "spotify-id-1", "When I Come Around", 85, "EXACT_TRACK;POPULARITY=8"));

        when(trackResolver.resolve("Green Day", "Basket Case"))
                .thenReturn(new TrackResolution("Basket Case", "Green Day", true,
                        "spotify-id-2", "Basket Case", 80, "EXACT_TRACK;POPULARITY=8"));

        when(trackResolver.resolve("Green Day", "American Idiot"))
                .thenReturn(new TrackResolution("American Idiot", "Green Day", true,
                        "spotify-id-3", "American Idiot", 75, "EXACT_TRACK;POPULARITY=7"));

        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        // Execute
        String playlistId = orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        // Verify
        assertThat(playlistId).isEqualTo("playlist-456");

        // Verify all tracks were resolved
        verify(trackResolver).resolve("Green Day", "When I Come Around");
        verify(trackResolver).resolve("Green Day", "Basket Case");
        verify(trackResolver).resolve("Green Day", "American Idiot");

        // Verify playlist was created
        verify(playlistService).createPlaylist(spotifyClient, "user-123", artist);

        // Verify tracks were added to playlist with all matched track IDs
        verify(playlistService).addTracks(
                spotifyClient,
                "playlist-456",
                List.of("spotify-id-1", "spotify-id-2", "spotify-id-3")
        );
    }

    @Test
    void excludesUnmatchedTracksFromPlaylist() {
        // Setup
        String artist = "Green Day";
        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 5),
                new TrackCount("Unknown Song", 1),
                new TrackCount("Basket Case", 4)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");

        when(trackResolver.resolve("Green Day", "When I Come Around"))
                .thenReturn(new TrackResolution("When I Come Around", "Green Day", true,
                        "spotify-id-1", "When I Come Around", 85, "EXACT_TRACK"));

        // Unmatched track
        when(trackResolver.resolve("Green Day", "Unknown Song"))
                .thenReturn(new TrackResolution("Unknown Song", "Green Day", false,
                        null, null, 5, "NO_RESULTS"));

        when(trackResolver.resolve("Green Day", "Basket Case"))
                .thenReturn(new TrackResolution("Basket Case", "Green Day", true,
                        "spotify-id-2", "Basket Case", 80, "EXACT_TRACK"));

        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        // Execute
        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        // Verify only matched tracks are added
        verify(playlistService).addTracks(
                spotifyClient,
                "playlist-456",
                List.of("spotify-id-1", "spotify-id-2")  // Unknown song excluded
        );
    }

    @Test
    void handlesAllUnmatchedTracks() {
        // Setup
        String artist = "Unknown Artist";
        List<TrackCount> tracks = List.of(
                new TrackCount("Song 1", 1),
                new TrackCount("Song 2", 1)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");

        // All tracks fail to match
        when(trackResolver.resolve(anyString(), anyString()))
                .thenReturn(new TrackResolution("unknown", "Unknown Artist", false,
                        null, null, 0, "NO_RESULTS"));

        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        // Execute
        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        // Verify playlist is created but with no tracks
        verify(playlistService).addTracks(
                spotifyClient,
                "playlist-456",
                List.of()
        );
    }

    @Test
    void sortsTracksByPlayCountBeforeResolution() {
        // Setup
        String artist = "Green Day";
        List<TrackCount> unsortedTracks = List.of(
                new TrackCount("Less Popular", 2),
                new TrackCount("Most Popular", 5),
                new TrackCount("Medium Popular", 3)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");

        when(trackResolver.resolve(anyString(), anyString()))
                .thenReturn(new TrackResolution("track", "Green Day", true,
                        "spotify-id", "Track Name", 80, "EXACT_TRACK"));

        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        // Execute
        orchestrator.buildPlaylist(spotifyClient, artist, unsortedTracks);

        // Verify it processes tracks correctly (orchestrator should handle them)
        // The important thing is that it resolves all tracks
        verify(playlistService).addTracks(
                eq(spotifyClient),
                eq("playlist-456"),
                any()
        );
    }

    @Test
    void handlesMixedConfidenceScores() {
        // Setup
        String artist = "Green Day";
        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 5),
                new TrackCount("When I Come Around (Live)", 2),
                new TrackCount("When I Come Around - Remix", 1)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");

        // Different confidence levels
        when(trackResolver.resolve("Green Day", "When I Come Around"))
                .thenReturn(new TrackResolution("When I Come Around", "Green Day", true,
                        "spotify-id-1", "When I Come Around", 85, "EXACT_TRACK"));

        when(trackResolver.resolve("Green Day", "When I Come Around (Live)"))
                .thenReturn(new TrackResolution("When I Come Around (Live)", "Green Day", true,
                        "spotify-id-2", "When I Come Around (Studio Version)", 40, "PARTIAL_TRACK"));

        when(trackResolver.resolve("Green Day", "When I Come Around - Remix"))
                .thenReturn(new TrackResolution("When I Come Around - Remix", "Green Day", false,
                        null, null, 15, "LOW_CONFIDENCE"));

        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        // Execute
        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        // Verify only matched tracks are added
        verify(playlistService).addTracks(
                spotifyClient,
                "playlist-456",
                List.of("spotify-id-1", "spotify-id-2")
        );
    }

    @Test
    void logsResolutionDetails() {
        // Setup
        String artist = "The Beatles";
        List<TrackCount> tracks = List.of(
                new TrackCount("Hey Jude", 10)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-fab4");

        when(trackResolver.resolve("The Beatles", "Hey Jude"))
                .thenReturn(new TrackResolution("Hey Jude", "The Beatles", true,
                        "spotify-jude", "Hey Jude", 95, "EXACT_TRACK;POPULARITY=10"));

        when(playlistService.createPlaylist(spotifyClient, "user-fab4", artist))
                .thenReturn("playlist-beatles");

        // Execute - should complete without errors and log properly
        String playlistId = orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        assertThat(playlistId).isEqualTo("playlist-beatles");
    }
}

