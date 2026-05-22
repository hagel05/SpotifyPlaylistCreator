package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.hagelbrand.data.TrackCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.*;

@Service
public class SetlistTrackService {
    private static final Logger log = LoggerFactory.getLogger(SetlistTrackService.class);

    public SetlistTrackService() {
    }

    /** Returns a map of track name → total play count across all setlists. */
    public Map<String, Long> getMostPlayedTracks(List<Setlist> setlists) {
        log.info("Getting most played tracks from Setlist");
        return setlists.stream()
                .filter(s -> s.sets() != null)
                .flatMap(s -> s.sets().set().stream())
                .flatMap(set -> set.song().stream())
                .map(SetlistSearchResponse.Song::name)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
    }

    /**
     * Returns a map of track name → original artist name for every song in the
     * setlists that setlist.fm has marked as a cover.  If the same track appears
     * as a cover in some setlists and as an original in others (very rare), the
     * first observed cover attribution wins.
     */
    public Map<String, String> getCoverArtistsByTrack(List<Setlist> setlists) {
        return setlists.stream()
                .filter(s -> s.sets() != null)
                .flatMap(s -> s.sets().set().stream())
                .flatMap(set -> set.song().stream())
                .filter(song -> song.name() != null && !song.name().isBlank()
                        && song.cover() != null && song.cover().name() != null)
                .collect(Collectors.toMap(
                        SetlistSearchResponse.Song::name,
                        song -> song.cover().name(),
                        (existing, replacement) -> existing   // keep first attribution
                ));
    }

    /**
     * Returns the top-N tracks by play count, enriched with cover-artist info.
     *
     * @param trackCounts  Map of track name → play count (from {@link #getMostPlayedTracks}).
     * @param coverArtists Map of track name → original artist (from {@link #getCoverArtistsByTrack}).
     * @param topN         Maximum number of results.
     */
    public List<TrackCount> getTopTracks(Map<String, Long> trackCounts,
                                         Map<String, String> coverArtists,
                                         int topN) {
        log.info("Sorting top {} tracks into most played order", topN);
        return trackCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new TrackCount(e.getKey(), e.getValue(), coverArtists.get(e.getKey())))
                .limit(topN)
                .toList();
    }

    /**
     * Convenience overload — no cover-artist enrichment.
     * Kept for backward compatibility with tests that don't supply cover-artist data.
     */
    public List<TrackCount> getTopTracks(Map<String, Long> trackCounts, int topN) {
        return getTopTracks(trackCounts, Map.of(), topN);
    }
}
