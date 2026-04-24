package org.hagelbrand.service.spotify;

import org.hagelbrand.data.SpotifySearchResponse;
import org.hagelbrand.data.TrackResolution;
import org.hagelbrand.service.setlistfm.SetlistFmServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;

@Service
public class SpotifyTrackResolver {

    private static final Logger log = LoggerFactory.getLogger(SpotifyTrackResolver.class);

    private static final int MIN_CONFIDENCE = 30;

    private final SpotifyServiceImpl spotifyService;

    public SpotifyTrackResolver(SpotifyServiceImpl spotifyService) {
        this.spotifyService = spotifyService;
    }

    public TrackResolution resolve(String artist, String track) {
        SpotifySearchResponse response =
                spotifyService.searchTrack(artist, track);

        if (response == null ||
                response.tracks() == null ||
                response.tracks().items().isEmpty()) {

            return new TrackResolution(
                    track, artist, false,
                    null, null,
                    0, "NO_RESULTS"
            );
        }

        log.debug("Track search returned response: {}", response.toString());

        String expected = normalize(track);

        List<TrackResolution> candidates =
                response.tracks().items().stream()
                        .map(item -> scoreCandidate(expected, artist, track, item))
                        .sorted(Comparator.comparingInt(TrackResolution::confidence).reversed())
                        .toList();

        TrackResolution best = candidates.getFirst();

        if (best.confidence() < MIN_CONFIDENCE) {
            return new TrackResolution(
                    track, artist, false,
                    null, null,
                    best.confidence(),
                    "LOW_CONFIDENCE"
            );
        }

        return best;
    }

    private TrackResolution scoreCandidate(
            String expectedNormalized,
            String artist,
            String originalTrack,
            SpotifySearchResponse.Item item
    ) {
        int score = 0;
        StringBuilder reason = new StringBuilder();

        String candidateTrack = normalize(item.name());
        String candidateAlbum =
                item.album() != null ? normalize(item.album().name()) : "";

        if (candidateTrack.equals(expectedNormalized)) {
            score += 50;
            reason.append("EXACT_TRACK;");
        }
        else if (candidateTrack.contains(expectedNormalized) ||
                expectedNormalized.contains(candidateTrack)) {
            score += 30;
            reason.append("PARTIAL_TRACK;");
        }

        if (!candidateAlbum.isEmpty() &&
                candidateAlbum.contains(expectedNormalized)) {
            score += 10;
            reason.append("ALBUM_MATCH;");
        }

        int popularityScore = Math.min(item.popularity() / 10, 10);
        score += popularityScore;
        reason.append("POPULARITY=").append(popularityScore);

        return new TrackResolution(
                originalTrack,
                artist,
                true,
                item.id(),
                item.name(),
                score,
                reason.toString()
        );
    }


    private String normalize(String value) {
        if (value == null) return "";

        String normalized =
                Normalizer.normalize(value, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", ""); // remove accents

        return normalized.toLowerCase()
                .replace("&", "and")               // & → and
                .replaceAll("[^a-z0-9 ]", "")      // remove punctuation
                .replaceAll("\\s+", " ")           // collapse whitespace
                .trim();
    }
}
