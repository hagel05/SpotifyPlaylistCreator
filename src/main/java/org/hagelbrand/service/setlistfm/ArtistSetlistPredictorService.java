package org.hagelbrand.service.setlistfm;

import org.hagelbrand.controller.PlaylistController;
import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.ArtistSearchResponse.Artist;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.hagelbrand.data.TrackCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ArtistSetlistPredictorService {
    private final SetlistFmService setlistFmService;
    private final SetlistTrackService trackService;
    private static final Logger log = LoggerFactory.getLogger(ArtistSetlistPredictorService.class);


    public ArtistSetlistPredictorService(SetlistFmService setlistFmService, SetlistTrackService trackService) {
        this.setlistFmService = setlistFmService;
        this.trackService = trackService;
    }


    public List<TrackCount> getTopTracksForArtist(
            String artistQuery,
            int limit
    ) {
        log.info("Getting Artists from search using setlist FM");
        ArtistSearchResponse artists =
                setlistFmService.getArtistsFromSearch(artistQuery);

        Artist artist =
                ArtistSearchFilterService.selectBest(artistQuery, artists);

        log.info("Getting setlists for artist {} using mbid {}", artist.name(), artist.mbid());
        List<Setlist> setlists =
                setlistFmService.getConcertSetlists(artist.mbid());

        Map<String, Long> counts =
                trackService.getMostPlayedTracks(setlists);

        return trackService.getTopTracks(counts, limit);
    }
}
