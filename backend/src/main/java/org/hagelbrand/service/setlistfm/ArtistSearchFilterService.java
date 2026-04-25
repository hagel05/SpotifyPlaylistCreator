package org.hagelbrand.service.setlistfm;

import org.hagelbrand.controller.PlaylistController;
import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.ArtistSearchResponse.Artist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class ArtistSearchFilterService {

    private final SetlistFmService setlistFmService;
    private static final int CONFIDENCE_THRESHOLD = 80;
    private static final Logger log = LoggerFactory.getLogger(ArtistSearchFilterService.class);

    public ArtistSearchFilterService(SetlistFmService setlistFmService) {
        this.setlistFmService = setlistFmService;
    }

    public Artist selectBest(String query, ArtistSearchResponse response) {
        int bestScore = 0;
        var topCandidates = new java.util.ArrayList<Artist>();

        // Single pass: score and track top candidates
        for (Artist artist : response.artists()) {
            int score = score(artist, query);

            if (score > bestScore) {
                bestScore = score;
                topCandidates.clear();
                topCandidates.add(artist);
            } else if (score >= bestScore - 5) {  // isNearTie inline
                topCandidates.add(artist);
            }
        }

        // If best score is weak, fail early
        if (bestScore < CONFIDENCE_THRESHOLD) {
            throw new IllegalStateException("No confident match for: " + query);
        }

        if (topCandidates.size() == 1) {
            return topCandidates.get(0);
        }

        // Tie-break: Use stable fallback (avoid N+1 API calls)
        // In rare tie scenarios, prefer artist with more disambiguation info or alphabetically first
        return topCandidates.stream()
                .max(Comparator
                        .comparingInt((Artist a) -> a.disambiguation().isEmpty() ? 0 : 1)
                        .thenComparing(Artist::name))
                .orElseThrow();
    }

    static boolean isNearTie(int score, int best) {
        return score >= best - 5; // small tolerance window
    }

    static record ScoredArtist(Artist artist, int score) {}


    static int score(Artist a, String query) {
        int score = 0;

        if (normalize(a.name()).equals(normalize(query))) score += 100;
        if (a.sortName().equalsIgnoreCase(query)) score += 25;
        if (!a.disambiguation().isBlank()) score += 5;
        if (containsCollaboration(a.name())) score -= 50;
        if (containsCollaboration(a.sortName())) score -= 50;
        if (looksLikeTribute(a)) score -= 50;

        return score;
    }

    static String normalize(String value) {
        return value.toLowerCase()
                .replaceAll("\\s+feat\\.?\\s+.*", "")
                .replaceAll("\\s+featuring\\s+.*", "")
                .replaceAll("\\s*&\\s+.*", "")
                .replaceAll(";.*", "")
                .trim();
    }

    private static boolean containsCollaboration(String value) {
        String v = value.toLowerCase();
        return v.contains("feat")
                || v.contains("&")
                || v.contains(";")
                || v.contains("featuring");
    }

    private static boolean looksLikeTribute(Artist artist) {
        String text = (artist.name() + " " + artist.disambiguation()).toLowerCase();

        return text.contains("tribute")
                || text.contains("cover band")
                || text.contains("tribute to")
                || text.contains("plays the music of");
    }

    private int getSetlistCount(Artist artist) {
        try {
            return setlistFmService
                    .getSetlistsFromSearch(artist.mbid())
                    .total();
        } catch (Exception e) {
            log.warn("Failed to fetch setlists for {}", artist.mbid(), e);
            return 0;
        }
    }
}

