package org.hagelbrand.controller;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.data.TrackCounts;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/setlist")
public class SetlistController {

    private final ArtistSetlistPredictorService artistSetlistPredictorService;

    public SetlistController(ArtistSetlistPredictorService artistSetlistPredictorService) {
        this.artistSetlistPredictorService = artistSetlistPredictorService;
    }

    @GetMapping("/{artist}/top-tracks")
    public TrackCounts getTopTracks(
            @PathVariable("artist") String artist,
            @RequestParam(defaultValue = "20") int limit) {
        List<TrackCount> tracks = artistSetlistPredictorService.getTopTracksForArtist(artist, limit);
        return new TrackCounts(tracks);
    }
}
