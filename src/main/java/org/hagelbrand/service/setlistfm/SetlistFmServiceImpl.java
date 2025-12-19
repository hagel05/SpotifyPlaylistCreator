package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class SetlistFmServiceImpl implements SetlistFmService {

    private final RestClient setlistFmClient;
    private final SetlistFilterService visibilityFilter;

    public SetlistFmServiceImpl(@Qualifier("setlistFmClient") RestClient setlistFmClient) {
        this.setlistFmClient = setlistFmClient;
        this.visibilityFilter = new SetlistFilterService();
    }

    @Override
    public ArtistSearchResponse getArtistsFromSearch(String artistName) {
        return setlistFmClient
                .get()
                .uri("/search/artists?artistName=" + artistName)
                .retrieve()
                .body(ArtistSearchResponse.class);
    }

    @Override
    public SetlistSearchResponse getSetlistsFromSearch(String mbid) {
        return setlistFmClient
                .get()
                .uri("/artist/" + mbid + "/setlists")
                .retrieve()
                .body(SetlistSearchResponse.class);
    }

    @Override
    public List<Setlist> getConcertSetlists(String mbid) {
        SetlistSearchResponse response = getSetlistsFromSearch(mbid);

        return response.setlists().stream()
                .filter(visibilityFilter::isVisibleConcert)
                .toList();
    }

    @Override
    public Setlist getSetlistById(String setlistId) {
        return setlistFmClient
                .get()
                .uri("/setlist/" + setlistId)
                .retrieve()
                .body(Setlist.class);
    }

}
