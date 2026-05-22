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

    // ── Helper: build an Album with no images (sufficient for scorer tests) ────

    private static SpotifySearchResponse.Album album(String id, String name) {
        return new SpotifySearchResponse.Album(id, name, null);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void matchesTrackWithExactName() {
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-123",
                                "When I Come Around",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                album("album-id", "Dookie"),
                                82
                        )
                ))
        );

        when(spotifyService.searchTrack("Green Day", "When I Come Around"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Green Day", "When I Come Around");

        assertThat(resolution.matched()).isTrue();
        assertThat(resolution.spotifyTrackId()).isEqualTo("spotify-id-123");
        assertThat(resolution.spotifyTrackName()).isEqualTo("When I Come Around");
        assertThat(resolution.confidence()).isGreaterThan(50);
        assertThat(resolution.reason()).contains("EXACT_TRACK");
    }

    @Test
    void matchesTrackWithPartialName() {
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-456",
                                "When I Come Around (Live)",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                album("album-id", "Live Album"),
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
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-789",
                                "Random Song",
                                List.of(new SpotifySearchResponse.Artist("Another Artist")),
                                album("album-id", "Album"),
                                10
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
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-accent",
                                "Clandestín",
                                List.of(new SpotifySearchResponse.Artist("Artist")),
                                album("album-id", "Album"),
                                70
                        )
                ))
        );

        when(spotifyService.searchTrack("Artist", "Clandestín"))
                .thenReturn(response);

        TrackResolution resolution = trackResolver.resolve("Artist", "Clandestín");

        assertThat(resolution.matched()).isTrue();
        assertThat(resolution.reason()).contains("EXACT_TRACK");
    }

    @Test
    void scoresPopularityCorrectly() {
        SpotifySearchResponse responseHighPopularity = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "id-high-pop",
                                "Song Name",
                                List.of(new SpotifySearchResponse.Artist("Artist")),
                                album("album-id", "Album"),
                                90
                        )
                ))
        );

        SpotifySearchResponse responseLowPopularity = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "id-low-pop",
                                "Song Name",
                                List.of(new SpotifySearchResponse.Artist("Artist")),
                                album("album-id", "Album"),
                                20
                        )
                ))
        );

        when(spotifyService.searchTrack("Artist", "Song Name"))
                .thenReturn(responseHighPopularity);
        int highPopScore = trackResolver.resolve("Artist", "Song Name").confidence();

        when(spotifyService.searchTrack("Artist", "Song Name"))
                .thenReturn(responseLowPopularity);
        int lowPopScore = trackResolver.resolve("Artist", "Song Name").confidence();

        assertThat(highPopScore).isGreaterThan(lowPopScore);
    }

    @Test
    void selectsBestCandidateFromMultipleResults() {
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "id-weak",
                                "Some Random Song",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                album("album-id", "Album"),
                                40
                        ),
                        new SpotifySearchResponse.Item(
                                "id-best",
                                "Basket Case",
                                List.of(new SpotifySearchResponse.Artist("Green Day")),
                                album("album-id", "Dookie"),
                                85
                        ),
                        new SpotifySearchResponse.Item(
                                "id-okay",
                                "Basket Case (Cover)",
                                List.of(new SpotifySearchResponse.Artist("Another Band")),
                                album("album-id", "Album"),
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
        SpotifySearchResponse response = new SpotifySearchResponse(
                new SpotifySearchResponse.Tracks(List.of(
                        new SpotifySearchResponse.Item(
                                "spotify-id-complex",
                                "Wannabe (Radio Edit Version 2)",
                                List.of(new SpotifySearchResponse.Artist("Spice Girls")),
                                album("album-id", "Spice"),
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
