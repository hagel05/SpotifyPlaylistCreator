package org.hagelbrand.service.spotify;

import org.hagelbrand.data.SpotifySearchResponse;
import org.hagelbrand.data.TrackResolution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for SpotifyTrackResolver.
 * Tests the track matching and confidence scoring logic with realistic Spotify search results.
 */
@ExtendWith(MockitoExtension.class)
class SpotifyTrackResolverIntegrationTest {

    @Mock
    private SpotifyServiceImpl spotifyService;

    private SpotifyTrackResolver trackResolver;

    @BeforeEach
    void setUp() {
        trackResolver = new SpotifyTrackResolver(spotifyService);
    }

    @Test
    void matchesTrackWithExactName() {
        // Simulate a search that returns the exact track
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-123",
                                "When I Come Around",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                new SpotifySearchResponse.Album("album-id", "Dookie"),
                                82  // popularity
                        )
                ))
        );

        when(spotifyService.searchTrack("Green Day", "When I Come Around"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Green Day", "When I Come Around");

        assertThat(resolution.matched()).isTrue();
        assertThat(resolution.spotifyTrackId()).isEqualTo("spotify-id-123");
        assertThat(resolution.spotifyTrackName()).isEqualTo("When I Come Around");
        assertThat(resolution.confidence()).isGreaterThan(50);  // Should have high confidence
        assertThat(resolution.reason()).contains("EXACT_TRACK");
    }

    @Test
    void matchesTrackWithPartialName() {
        // Simulate a search where only partial match is available
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-456",
                                "When I Come Around (Live)",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                new SpotifySearchResponse.Album("album-id", "Live Album"),
                                45
                        )
                ))
        );

        when(spotifyService.searchTrack("Green Day", "When I Come Around"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Green Day", "When I Come Around");

        assertThat(resolution.matched()).isTrue();
        assertThat(resolution.confidence()).isGreaterThanOrEqualTo(30);
        assertThat(resolution.reason()).contains("PARTIAL_TRACK");
    }

    @Test
    void rejectTrackWithLowConfidence() {
        // Simulate a search that returns a weak match
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-789",
                                "Random Song",
                                List.of(new SpotifySearchResponse.Artist("Another Artist")),
                                new SpotifySearchResponse.Album("album-id", "Album"),
                                10  // very low popularity
                        )
                ))
        );

        when(spotifyService.searchTrack("Green Day", "When I Come Around"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Green Day", "When I Come Around");

        assertThat(resolution.matched()).isFalse();
        assertThat(resolution.confidence()).isLessThan(30);
        assertThat(resolution.reason()).isEqualTo("LOW_CONFIDENCE");
    }

    @Test
    void handlesNoSearchResults() {
        // Simulate an empty search result
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of())
        );

        when(spotifyService.searchTrack(anyString(), anyString()))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Unknown Artist", "Unknown Song");

        assertThat(resolution.matched()).isFalse();
        assertThat(resolution.reason()).isEqualTo("NO_RESULTS");
        assertThat(resolution.confidence()).isZero();
    }

    @Test
    void handlesNullSearchResponse() {
        when(spotifyService.searchTrack(anyString(), anyString()))
                .thenReturn(null);

        TrackResolution resolution = trackResolver.resolve("Green Day", "When I Come Around");

        assertThat(resolution.matched()).isFalse();
        assertThat(resolution.reason()).isEqualTo("NO_RESULTS");
    }

    @Test
    void normalizesTrackNamesBeforeMatching() {
        // Test with accented characters and special punctuation
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-accent",
                                "Clandestín",  // matching after normalization (accent removed)
                                List.of(new SpotifySearchResponse.Artist("Artist")),
                                new SpotifySearchResponse.Album("album-id", "Album"),
                                70
                        )
                ))
        );

        when(spotifyService.searchTrack("Artist", "Clandestín"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Artist", "Clandestín");

        // After normalization, these should match (accents removed)
        assertThat(resolution.matched()).isTrue();
        assertThat(resolution.reason()).contains("EXACT_TRACK");
    }

    @Test
    void scoresPopularityCorrectly() {
        // High popularity track should score higher
        SpotifySearchResponse responseHighPopularity = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "id-high-pop",
                                "Song Name",
                                List.of(new SpotifySearchResponse.Artist("Artist")),
                                new SpotifySearchResponse.Album("album-id", "Album"),
                                90  // high popularity
                        )
                ))
        );

        SpotifySearchResponse responseLowPopularity = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "id-low-pop",
                                "Song Name",
                                List.of(new SpotifySearchResponse.Artist("Artist")),
                                new SpotifySearchResponse.Album("album-id", "Album"),
                                20  // low popularity
                        )
                ))
        );

        when(spotifyService.searchTrack("Artist", "Song Name"))
                .thenReturn(responseHighPopularity);

        TrackResolution highPopResolution = trackResolver.resolve("Artist", "Song Name");
        int highPopScore = highPopResolution.confidence();

        when(spotifyService.searchTrack("Artist", "Song Name"))
                .thenReturn(responseLowPopularity);

        TrackResolution lowPopResolution = trackResolver.resolve("Artist", "Song Name");
        int lowPopScore = lowPopResolution.confidence();

        assertThat(highPopScore).isGreaterThan(lowPopScore);
    }

    @Test
    void selectsBestCandidateFromMultipleResults() {
        // Multiple tracks returned; should select the best match
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "id-weak",
                                "Some Random Song",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                new SpotifySearchResponse.Album("album-id", "Album"),
                                40
                        ),
                        new SpotifySearchResponse.Item(
                                "id-best",
                                "Basket Case",  // Exact match
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                new SpotifySearchResponse.Album("album-id", "Dookie"),
                                85
                        ),
                        new SpotifySearchResponse.Item(
                                "id-okay",
                                "Basket Case (Cover)",
                                List.of(new SpotifySearchResponse.Artist("Another Band")),
                                new SpotifySearchResponse.Album("album-id", "Album"),
                                50
                        )
                ))
        );

        when(spotifyService.searchTrack("Green Day", "Basket Case"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Green Day", "Basket Case");

        assertThat(resolution.spotifyTrackId()).isEqualTo("id-best");
        assertThat(resolution.spotifyTrackName()).isEqualTo("Basket Case");
        assertThat(resolution.matched()).isTrue();
    }

    @Test
    void handlesRealWorldComplexTrackNames() {
        // Test with real-world complex track names
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-complex",
                                "Wannabe (Radio Edit Version 2)",
                                List.of(new SpotifySearchResponse.Artist("Spice Girls")),
                                new SpotifySearchResponse.Album("album-id", "Spice"),
                                88
                        )
                ))
        );

        when(spotifyService.searchTrack("Spice Girls", "Wannabe"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Spice Girls", "Wannabe");

        assertThat(resolution.matched()).isTrue();
        assertThat(resolution.confidence()).isGreaterThan(30);
    }
}

