package org.hagelbrand.service.spotify;

import org.hagelbrand.data.AlternativeTrack;
import org.hagelbrand.data.SpotifySearchResponse;
import org.hagelbrand.data.TrackCount;
import org.hagelbrand.data.TrackPreview;
import org.hagelbrand.data.TrackResolution;
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
    private static final int MAX_ALTERNATIVES = 4;

    private final SpotifyServiceImpl spotifyService;

    public SpotifyTrackResolver(SpotifyServiceImpl spotifyService) {
        this.spotifyService = spotifyService;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Resolves a single setlist.fm track to its best Spotify match.
     * When {@code coverArtist} is non-null (i.e. the performing artist is covering
     * someone else's song), the search is run under the original artist's name —
     * that is the version that actually exists on Spotify.
     *
     * @param performingArtist The artist whose setlist we are processing.
     * @param track            Track name from setlist.fm.
     * @param coverArtist      Original recording artist if this is a cover; {@code null} otherwise.
     */
    public TrackResolution resolve(String performingArtist, String track, String coverArtist) {
        String searchArtist = coverArtist != null ? coverArtist : performingArtist;
        SpotifySearchResponse response = spotifyService.searchTrack(searchArtist, track);
        return pickBest(track, searchArtist, response);
    }

    /** Convenience overload — no cover artist (original song). */
    public TrackResolution resolve(String artist, String track) {
        return resolve(artist, track, null);
    }

    /**
     * Like {@link #resolve} but also returns up to {@value MAX_ALTERNATIVES} runner-up
     * candidates so the user can swap a bad match in the frontend preview step.
     * The returned {@link TrackPreview} is ready to be serialised directly to the client.
     */
    public TrackPreview resolveWithAlternatives(String performingArtist, TrackCount trackCount) {
        String searchArtist = trackCount.coverArtist() != null
                ? trackCount.coverArtist()
                : performingArtist;

        SpotifySearchResponse response = spotifyService.searchTrack(searchArtist, trackCount.track());

        if (response == null
                || response.tracks() == null
                || response.tracks().items().isEmpty()) {
            return new TrackPreview(
                    trackCount.track(), trackCount.plays(), trackCount.coverArtist(),
                    false, null, null, null, 0, "NO_RESULTS", List.of(), null);
        }

        String expected = normalize(trackCount.track());

        List<ScoredItem> scored = response.tracks().items().stream()
                .map(item -> new ScoredItem(item, scoreCandidate(expected, searchArtist, trackCount.track(), item)))
                .sorted(Comparator.comparingInt(si -> -si.resolution().confidence()))
                .toList();

        ScoredItem best = scored.getFirst();

        if (best.resolution().confidence() < MIN_CONFIDENCE) {
            return new TrackPreview(
                    trackCount.track(), trackCount.plays(), trackCount.coverArtist(),
                    false, null, null, null, best.resolution().confidence(), "LOW_CONFIDENCE",
                    buildAlternatives(scored, Integer.MAX_VALUE), null);  // show all as alternatives
        }

        List<AlternativeTrack> alternatives = buildAlternatives(scored.subList(1, scored.size()), MAX_ALTERNATIVES);

        TrackResolution bestMatch = best.resolution();
        return new TrackPreview(
                trackCount.track(), trackCount.plays(), trackCount.coverArtist(),
                true,
                bestMatch.spotifyTrackId(),
                bestMatch.spotifyTrackName(),
                bestMatch.spotifyArtistName(),
                bestMatch.confidence(),
                bestMatch.reason(),
                alternatives,
                bestMatch.albumImageUrl());
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private TrackResolution pickBest(String originalTrack, String searchArtist,
                                     SpotifySearchResponse response) {
        if (response == null
                || response.tracks() == null
                || response.tracks().items().isEmpty()) {
            return unmatched(originalTrack, searchArtist, 0, "NO_RESULTS");
        }

        log.debug("Track search returned response: {}", response);

        String expected = normalize(originalTrack);

        List<TrackResolution> candidates = response.tracks().items().stream()
                .map(item -> scoreCandidate(expected, searchArtist, originalTrack, item))
                .sorted(Comparator.comparingInt(TrackResolution::confidence).reversed())
                .toList();

        TrackResolution best = candidates.getFirst();

        if (best.confidence() < MIN_CONFIDENCE) {
            return unmatched(originalTrack, searchArtist, best.confidence(), "LOW_CONFIDENCE");
        }

        return best;
    }

    private TrackResolution scoreCandidate(String expectedNormalized, String artist,
                                           String originalTrack, SpotifySearchResponse.Item item) {
        int score = 0;
        StringBuilder reason = new StringBuilder();

        String candidateTrack = normalize(item.name());
        String candidateAlbum = item.album() != null ? normalize(item.album().name()) : "";

        if (candidateTrack.equals(expectedNormalized)) {
            score += 50;
            reason.append("EXACT_TRACK;");
        } else if (candidateTrack.contains(expectedNormalized)
                || expectedNormalized.contains(candidateTrack)) {
            score += 30;
            reason.append("PARTIAL_TRACK;");
        }

        if (!candidateAlbum.isEmpty() && candidateAlbum.contains(expectedNormalized)) {
            score += 10;
            reason.append("ALBUM_MATCH;");
        }

        int popularityScore = Math.min(item.popularity() / 10, 10);
        score += popularityScore;
        reason.append("POPULARITY=").append(popularityScore);

        String artistName = item.artists() != null && !item.artists().isEmpty()
                ? item.artists().getFirst().name()
                : null;

        // Spotify returns images sorted by size descending [640, 300, 64].
        // Use the smallest thumbnail available — plenty sharp for a 40×40 display slot.
        String albumImageUrl = null;
        if (item.album() != null
                && item.album().images() != null
                && !item.album().images().isEmpty()) {
            albumImageUrl = item.album().images().getLast().url();
        }

        return new TrackResolution(
                originalTrack, artist, true,
                item.id(), item.name(), artistName,
                score, reason.toString(), albumImageUrl);
    }

    private List<AlternativeTrack> buildAlternatives(List<ScoredItem> candidates, int limit) {
        return candidates.stream()
                .filter(si -> si.resolution().confidence() > 0)
                .limit(limit)
                .map(si -> new AlternativeTrack(
                        si.resolution().spotifyTrackId(),
                        si.resolution().spotifyTrackName(),
                        si.resolution().spotifyArtistName(),
                        si.resolution().confidence(),
                        si.resolution().albumImageUrl()))
                .toList();
    }

    private TrackResolution unmatched(String track, String artist, int confidence, String reason) {
        return new TrackResolution(track, artist, false, null, null, null, confidence, reason, null);
    }

    private String normalize(String value) {
        if (value == null) return "";

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");          // remove accents

        return normalized.toLowerCase()
                .replace("&", "and")                 // & → and
                .replaceAll("[^a-z0-9 ]", "")        // remove punctuation
                .replaceAll("\\s+", " ")             // collapse whitespace
                .trim();
    }

    /** Pairs a raw Spotify item with its scored resolution so we can keep both. */
    private record ScoredItem(SpotifySearchResponse.Item item, TrackResolution resolution) {}
}
