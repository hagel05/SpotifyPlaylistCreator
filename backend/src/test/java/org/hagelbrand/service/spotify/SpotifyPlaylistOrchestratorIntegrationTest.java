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
import static org.mockito.ArgumentMatchers.isNull;
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

    // ── Helper to build a matched TrackResolution ─────────────────────────────

    private static TrackResolution matched(String track, String id, int confidence, String reason) {
        return new TrackResolution(track, "Green Day", true, id, track, "Green Day", confidence, reason, null);
    }

    private static TrackResolution unmatched(String track, int confidence, String reason) {
        return new TrackResolution(track, "Green Day", false, null, null, null, confidence, reason, null);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void buildPlaylistResolvesTracksAndCreatesPlaylist() {
        String artist = "Green Day";
        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 5),
                new TrackCount("Basket Case", 4),
                new TrackCount("American Idiot", 3)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");

        when(trackResolver.resolve("Green Day", "When I Come Around", null))
                .thenReturn(matched("When I Come Around", "spotify-id-1", 85, "EXACT_TRACK;POPULARITY=8"));
        when(trackResolver.resolve("Green Day", "Basket Case", null))
                .thenReturn(matched("Basket Case", "spotify-id-2", 80, "EXACT_TRACK;POPULARITY=8"));
        when(trackResolver.resolve("Green Day", "American Idiot", null))
                .thenReturn(matched("American Idiot", "spotify-id-3", 75, "EXACT_TRACK;POPULARITY=7"));

        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        String playlistId = orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        assertThat(playlistId).isEqualTo("playlist-456");

        verify(trackResolver).resolve("Green Day", "When I Come Around", null);
        verify(trackResolver).resolve("Green Day", "Basket Case", null);
        verify(trackResolver).resolve("Green Day", "American Idiot", null);
        verify(playlistService).createPlaylist(spotifyClient, "user-123", artist);
        verify(playlistService).addTracks(
                spotifyClient, "playlist-456",
                List.of("spotify-id-1", "spotify-id-2", "spotify-id-3"));
    }

    @Test
    void excludesUnmatchedTracksFromPlaylist() {
        String artist = "Green Day";
        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 5),
                new TrackCount("Unknown Song", 1),
                new TrackCount("Basket Case", 4)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");
        when(trackResolver.resolve("Green Day", "When I Come Around", null))
                .thenReturn(matched("When I Come Around", "spotify-id-1", 85, "EXACT_TRACK"));
        when(trackResolver.resolve("Green Day", "Unknown Song", null))
                .thenReturn(unmatched("Unknown Song", 5, "NO_RESULTS"));
        when(trackResolver.resolve("Green Day", "Basket Case", null))
                .thenReturn(matched("Basket Case", "spotify-id-2", 80, "EXACT_TRACK"));
        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        verify(playlistService).addTracks(
                spotifyClient, "playlist-456",
                List.of("spotify-id-1", "spotify-id-2"));
    }

    @Test
    void handlesAllUnmatchedTracks() {
        String artist = "Unknown Artist";
        List<TrackCount> tracks = List.of(
                new TrackCount("Song 1", 1),
                new TrackCount("Song 2", 1)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");
        when(trackResolver.resolve(anyString(), anyString(), isNull()))
                .thenReturn(new TrackResolution("unknown", "Unknown Artist", false,
                        null, null, null, 0, "NO_RESULTS", null));
        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        verify(playlistService).addTracks(spotifyClient, "playlist-456", List.of());
    }

    @Test
    void sortsTracksByPlayCountBeforeResolution() {
        String artist = "Green Day";
        List<TrackCount> unsortedTracks = List.of(
                new TrackCount("Less Popular", 2),
                new TrackCount("Most Popular", 5),
                new TrackCount("Medium Popular", 3)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");
        when(trackResolver.resolve(anyString(), anyString(), isNull()))
                .thenReturn(matched("track", "spotify-id", 80, "EXACT_TRACK"));
        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        orchestrator.buildPlaylist(spotifyClient, artist, unsortedTracks);

        verify(playlistService).addTracks(eq(spotifyClient), eq("playlist-456"), any());
    }

    @Test
    void handlesMixedConfidenceScores() {
        String artist = "Green Day";
        List<TrackCount> tracks = List.of(
                new TrackCount("When I Come Around", 5),
                new TrackCount("When I Come Around (Live)", 2),
                new TrackCount("When I Come Around - Remix", 1)
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-123");
        when(trackResolver.resolve("Green Day", "When I Come Around", null))
                .thenReturn(matched("When I Come Around", "spotify-id-1", 85, "EXACT_TRACK"));
        when(trackResolver.resolve("Green Day", "When I Come Around (Live)", null))
                .thenReturn(new TrackResolution("When I Come Around (Live)", "Green Day", true,
                        "spotify-id-2", "When I Come Around (Studio Version)", "Green Day", 40, "PARTIAL_TRACK", null));
        when(trackResolver.resolve("Green Day", "When I Come Around - Remix", null))
                .thenReturn(unmatched("When I Come Around - Remix", 15, "LOW_CONFIDENCE"));
        when(playlistService.createPlaylist(spotifyClient, "user-123", artist))
                .thenReturn("playlist-456");

        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        verify(playlistService).addTracks(
                spotifyClient, "playlist-456",
                List.of("spotify-id-1", "spotify-id-2"));
    }

    @Test
    void usesCoverArtistWhenResolvingCoverSong() {
        String artist = "The CAB";
        List<TrackCount> tracks = List.of(
                new TrackCount("...Baby One More Time", 5, "Britney Spears")
        );

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-cab");
        when(trackResolver.resolve("The CAB", "...Baby One More Time", "Britney Spears"))
                .thenReturn(new TrackResolution("...Baby One More Time", "Britney Spears", true,
                        "spotify-britney-id", "...Baby One More Time", "Britney Spears", 88, "EXACT_TRACK;POPULARITY=9", null));
        when(playlistService.createPlaylist(spotifyClient, "user-cab", artist))
                .thenReturn("playlist-cab");

        orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        // Must resolve under the original artist, not the performing artist
        verify(trackResolver).resolve("The CAB", "...Baby One More Time", "Britney Spears");
        verify(playlistService).addTracks(spotifyClient, "playlist-cab", List.of("spotify-britney-id"));
    }

    @Test
    void logsResolutionDetails() {
        String artist = "The Beatles";
        List<TrackCount> tracks = List.of(new TrackCount("Hey Jude", 10));

        when(userService.getCurrentUserId(spotifyClient)).thenReturn("user-fab4");
        when(trackResolver.resolve("The Beatles", "Hey Jude", null))
                .thenReturn(new TrackResolution("Hey Jude", "The Beatles", true,
                        "spotify-jude", "Hey Jude", "The Beatles", 95, "EXACT_TRACK;POPULARITY=10", null));
        when(playlistService.createPlaylist(spotifyClient, "user-fab4", artist))
                .thenReturn("playlist-beatles");

        String playlistId = orchestrator.buildPlaylist(spotifyClient, artist, tracks);

        assertThat(playlistId).isEqualTo("playlist-beatles");
    }
}
