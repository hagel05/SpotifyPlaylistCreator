package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RateLimitedSetlistFetcher {

    // TODO determine if I want to use this or trash it
    private final SetlistFmService setlistFmService;

    // Daily quota and per-second limits
    private static final int MAX_PER_SECOND = 2;
    private static final int MAX_PER_DAY = 1440;

    // Track daily usage
    private int requestsToday = 0;
    private long lastReset = System.currentTimeMillis();

    public RateLimitedSetlistFetcher(SetlistFmService setlistFmService) {
        this.setlistFmService = setlistFmService;
    }

    public List<Setlist> fetchSetlistsRateLimited(List<Setlist> setlistsToFetch) {
        List<Setlist> results = new ArrayList<>();

        for (Setlist s : setlistsToFetch) {
            // Respect daily limit
            if (requestsToday >= MAX_PER_DAY) {
                System.out.println("Reached daily limit, stopping requests.");
                break;
            }

            try {
                // Fetch setlist
                Setlist fullSetlist = setlistFmService.getSetlistById(s.id());
                results.add(fullSetlist);

                requestsToday++;

                // Respect 2 requests per second
                Thread.sleep(500); // 1000ms / 2 req/sec
            } catch (HttpClientErrorException.TooManyRequests e) {
                System.err.println("429 Too Many Requests for " + s.id() + ", sleeping 1s and retrying...");
                try { Thread.sleep(1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                System.err.println("Failed to fetch setlist " + s.id() + ": " + e.getMessage());
            }
        }

        return results;
    }

    public Map<String, Long> getMostPlayedTracks(List<Setlist> fetchedSetlists) {
        return fetchedSetlists.stream()
                .filter(s -> s.sets() != null)
                .flatMap(s -> s.sets().set().stream())
                .flatMap(set -> set.song().stream())
                .map(SetlistSearchResponse.Song::name)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
    }

    public List<Map.Entry<String, Long>> getTopTracks(Map<String, Long> trackCounts, int topN) {
        return trackCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // descending
                .limit(topN)
                .collect(Collectors.toList());
    }
}
