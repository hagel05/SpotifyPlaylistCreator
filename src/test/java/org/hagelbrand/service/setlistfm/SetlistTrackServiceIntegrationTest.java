package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.TrackCount;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SetlistTrackService.
 * Tests track aggregation and ranking logic with realistic setlist data.
 */
class SetlistTrackServiceIntegrationTest {

    private final SetlistTrackService setlistTrackService = new SetlistTrackService();

    @Test
    void aggregatesMostPlayedTracksFromMultipleSetlists() {
        List<SetlistSearchResponse.Setlist> setlists = List.of(
                createMockSetlist("id1", List.of("Song A", "Song B", "Song C")),
                createMockSetlist("id2", List.of("Song A", "Song B", "Song D")),
                createMockSetlist("id3", List.of("Song A", "Song E", "Song F"))
        );

        Map<String, Long> trackCounts = setlistTrackService.getMostPlayedTracks(setlists);

        assertThat(trackCounts)
                .containsEntry("Song A", 3L)
                .containsEntry("Song B", 2L)
                .containsEntry("Song C", 1L)
                .containsEntry("Song D", 1L)
                .containsEntry("Song E", 1L)
                .containsEntry("Song F", 1L);
    }

    @Test
    void filtersOutSetlistsWithNullSets() {
        // Create a mock setlist with null sets
        SetlistSearchResponse.Setlist setlistWithNullSets =
                new SetlistSearchResponse.Setlist(
                        null, null, null, null, null, null, "id1", "v1", "2023-01-01", "2023-01-02T00:00:00Z"
                );

        List<SetlistSearchResponse.Setlist> setlists = List.of(
                setlistWithNullSets,
                createMockSetlist("id2", List.of("Song A", "Song B"))
        );

        Map<String, Long> trackCounts = setlistTrackService.getMostPlayedTracks(setlists);

        // Should only count songs from the valid setlist
        assertThat(trackCounts)
                .containsEntry("Song A", 1L)
                .containsEntry("Song B", 1L)
                .hasSize(2);
    }

    @Test
    void ranksTopTracksCorrectly() {
        Map<String, Long> trackCounts = Map.of(
                "Song A", 5L,
                "Song B", 3L,
                "Song C", 8L,
                "Song D", 2L,
                "Song E", 1L
        );

        List<TrackCount> topTracks = setlistTrackService.getTopTracks(trackCounts, 3);

        assertThat(topTracks).hasSize(3);
        assertThat(topTracks.get(0)).extracting(TrackCount::track, TrackCount::plays)
                .containsExactly("Song C", 8L);
        assertThat(topTracks.get(1)).extracting(TrackCount::track, TrackCount::plays)
                .containsExactly("Song A", 5L);
        assertThat(topTracks.get(2)).extracting(TrackCount::track, TrackCount::plays)
                .containsExactly("Song B", 3L);
    }

    @Test
    void limitsResultsToRequestedNumber() {
        Map<String, Long> trackCounts = Map.of(
                "Track 1", 10L,
                "Track 2", 9L,
                "Track 3", 8L,
                "Track 4", 7L,
                "Track 5", 6L
        );

        List<TrackCount> top2 = setlistTrackService.getTopTracks(trackCounts, 2);
        List<TrackCount> top5 = setlistTrackService.getTopTracks(trackCounts, 5);

        assertThat(top2).hasSize(2);
        assertThat(top5).hasSize(5);
    }

    @Test
    void handlesEmptySetlists() {
        List<SetlistSearchResponse.Setlist> emptySetlists = List.of();

        Map<String, Long> trackCounts = setlistTrackService.getMostPlayedTracks(emptySetlists);

        assertThat(trackCounts).isEmpty();
    }

    @Test
    void handlesSingleSetlist() {
        List<SetlistSearchResponse.Setlist> setlists = List.of(
                createMockSetlist("id1", List.of("Song A", "Song B", "Song C"))
        );

        Map<String, Long> trackCounts = setlistTrackService.getMostPlayedTracks(setlists);

        assertThat(trackCounts)
                .hasSize(3)
                .allSatisfy((track, count) -> assertThat(count).isEqualTo(1L));
    }

    @Test
    void realWorldScenarioWithComplexSetlist() {
        // Simulate a realistic scenario: multiple setlists with different structures
        List<SetlistSearchResponse.Setlist> setlists = List.of(
                createMockSetlist("setlist1", List.of("Billie Jean", "She", "When I Come Around")),
                createMockSetlist("setlist2", List.of("Billie Jean", "She", "Basket Case", "American Idiot")),
                createMockSetlist("setlist3", List.of("Billie Jean", "When I Come Around", "American Idiot"))
        );

        Map<String, Long> trackCounts = setlistTrackService.getMostPlayedTracks(setlists);
        List<TrackCount> topTracks = setlistTrackService.getTopTracks(trackCounts, 3);

        // Verify the ranking makes sense - we're asking for top 3
        assertThat(topTracks).hasSize(3);
        assertThat(topTracks.get(0)).extracting(TrackCount::track, TrackCount::plays)
                .containsExactly("Billie Jean", 3L);

        // The next two should be from tracks that appear 2 times
        assertThat(topTracks).extracting(TrackCount::plays)
                .containsExactly(3L, 2L, 2L);
    }

    /**
     * Helper method to create a mock setlist with the given songs
     */
    private SetlistSearchResponse.Setlist createMockSetlist(String id, List<String> songs) {
        List<SetlistSearchResponse.Song> songList = songs.stream()
                .map(name -> new SetlistSearchResponse.Song(name, false, null))
                .toList();

        SetlistSearchResponse.Set set = new SetlistSearchResponse.Set(
                "Set 1", null, songList
        );

        SetlistSearchResponse.Sets sets = new SetlistSearchResponse.Sets(List.of(set));

        return new SetlistSearchResponse.Setlist(
                null, null, null, sets, null, null, id, "v1", "2023-01-01", "2023-01-02T00:00:00Z"
        );
    }
}

