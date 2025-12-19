package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.hagelbrand.data.TrackCount;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.*;

@Service
public class SetlistTrackService {

    public SetlistTrackService() {
    }

    public Map<String, Long> getMostPlayedTracks(List<Setlist> setlists) {
        return setlists.stream()
                .filter(s -> s.sets() != null) // skip setlists with null sets
                .flatMap(s -> s.sets().set().stream()) // all sets in this setlist
                .flatMap(set -> set.song().stream()) // all songs in this set
                .map(SetlistSearchResponse.Song::name) // get song name
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
    }

    public List<TrackCount> getTopTracks(Map<String, Long> trackCounts, int topN) {
        return trackCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new TrackCount(e.getKey(), e.getValue()))
                .limit(topN)
                .toList();
    }
}
