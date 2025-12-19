package org.hagelbrand.service.spotify;

import org.hagelbrand.data.SpotifySearchResponse;
import org.hagelbrand.data.TrackResolution;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Optional;

@Service
public class SpotifyTrackResolver {

    private final SpotifyServiceImpl spotifyService;

    public SpotifyTrackResolver(SpotifyServiceImpl spotifyService) {
        this.spotifyService = spotifyService;
    }

    public TrackResolution resolveTrackId(String artist, String track) {
        SpotifySearchResponse response = spotifyService.searchTrack(artist, track);

        if (response == null ||
                response.tracks() == null ||
                response.tracks().items().isEmpty()) {
            return new TrackResolution(track, artist,false, null, null, 0, "NO_RESULTS");
        }

        var bestMatch = response.tracks().items().stream()
                .map(item -> {
                    int score = 0;
                    String reason = "";

                    if (exactMatch(track, item.name())) {
                        score += 50;
                        reason += "EXACT_TRACK_MATCH;";
                    } else if (normalize(item.name()).contains(normalize(track))) {
                        score += 20;
                        reason += "PARTIAL_TRACK_MATCH;";
                    }

                    if (normalize(item.album().name()).contains(normalize(track))) {
                        score += 10;
                        reason += "ALBUM_MATCH;";
                    }

                    // Popularity contribution (0–10)
                    int popularityScore = Math.min(item.popularity() / 10, 10);
                    score += popularityScore;
                    reason += "POPULARITY:" + popularityScore;

                    return new TrackResolution(
                            track,
                            artist,
                            true,
                            item.id(),
                            item.name(),
                            score,
                            reason
                    );
                })
                .max((a, b) -> Integer.compare(a.confidence(), b.confidence()))
                .get();

        SpotifySearchResponse.Item item = response.tracks().items().getFirst();

        if (bestMatch.confidence() < 30) {
            return new TrackResolution(track, artist, false, null, null, bestMatch.confidence(), "LOW_CONFIDENCE");
        }

        return bestMatch;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove accents
        normalized = normalized.toLowerCase()
                .replaceAll("&", "and")
                .replaceAll("\\(.*?\\)", "")
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }


    private boolean exactMatch(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

}
