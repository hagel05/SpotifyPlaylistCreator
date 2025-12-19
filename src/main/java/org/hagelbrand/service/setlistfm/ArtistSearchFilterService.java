package org.hagelbrand.service.setlistfm;

import org.hagelbrand.controller.PlaylistController;
import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.ArtistSearchResponse.Artist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class ArtistSearchFilterService {

    private static final int CONFIDENCE_THRESHOLD = 80;
    private static final Logger log = LoggerFactory.getLogger(ArtistSearchFilterService.class);


    public static Artist selectBest(String query, ArtistSearchResponse response) {
        return response.artists().stream()
                .max(Comparator.comparingInt(a -> score(a, query)))
                .filter(a -> score(a, query) >= CONFIDENCE_THRESHOLD)
                .orElseThrow(() -> new IllegalStateException(
                        "No confident artist match for query: " + query
                ));
    }


    static int score(Artist a, String query) {
        log.info("Scoring artists for normalization");
        int score = 0;

        if (a.name().equalsIgnoreCase(query)) score += 100;
        if (!a.disambiguation().isBlank()) score += 20;
        if (!containsFeat(a.name())) score += 10;
        if (!containsFeat(a.sortName())) score += 10;

        return score;
    }

    private static boolean containsFeat(String value) {
        return value.toLowerCase().contains("feat");
    }
}

