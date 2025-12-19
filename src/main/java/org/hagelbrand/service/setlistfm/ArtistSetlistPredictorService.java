package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.ArtistSearchResponse.Artist;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.hagelbrand.data.TrackCount;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ArtistSetlistPredictorService {
    private final SetlistFmService setlistFmService;
    private final SetlistTrackService trackService;

    public ArtistSetlistPredictorService(SetlistFmService setlistFmService, SetlistTrackService trackService) {
        this.setlistFmService = setlistFmService;
        this.trackService = trackService;
    }


    public List<TrackCount> getTopTracksForArtist(
            String artistQuery,
            int limit
    ) {
        ArtistSearchResponse artists =
                setlistFmService.getArtistsFromSearch(artistQuery);

        Artist artist =
                ArtistSearchFilterService.selectBest(artistQuery, artists);

        List<Setlist> setlists =
                setlistFmService.getConcertSetlists(artist.mbid());

        Map<String, Long> counts =
                trackService.getMostPlayedTracks(setlists);

        return trackService.getTopTracks(counts, limit);
    }
}
