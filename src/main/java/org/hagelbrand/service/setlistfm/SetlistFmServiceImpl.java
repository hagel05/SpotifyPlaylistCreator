package org.hagelbrand.service.setlistfm;

import org.hagelbrand.controller.PlaylistController;
import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class SetlistFmServiceImpl implements SetlistFmService {

    private final RestClient setlistFmClient;
    private final SetlistFilterService visibilityFilter;

    private static final Logger log = LoggerFactory.getLogger(SetlistFmServiceImpl.class);


    public SetlistFmServiceImpl(@Qualifier("setlistFmClient") RestClient setlistFmClient) {
        this.setlistFmClient = setlistFmClient;
        this.visibilityFilter = new SetlistFilterService();
    }

    @Override
    public ArtistSearchResponse getArtistsFromSearch(String artistName) {
        log.info("Searching artists from setlist.fm");
        return setlistFmClient
                .get()
                .uri("/search/artists?artistName=" + artistName)
                .retrieve()
                .body(ArtistSearchResponse.class);
    }

    @Override
    public SetlistSearchResponse getSetlistsFromSearch(String mbid) {
        log.info("Getting artist using mbid from setlist.fm");
        return setlistFmClient
                .get()
                .uri("/artist/" + mbid + "/setlists")
                .retrieve()
                .body(SetlistSearchResponse.class);
    }

    @Override
    public List<Setlist> getConcertSetlists(String mbid) {
        log.info("Filtering out live or promotional appearances using search response");
        SetlistSearchResponse response = getSetlistsFromSearch(mbid);

        return response.setlists().stream()
                .filter(visibilityFilter::isVisibleConcert)
                .toList();
    }

    @Override
    public Setlist getSetlistById(String setlistId) {
        log.info("Getting setlist with id {}", setlistId);
        return setlistFmClient
                .get()
                .uri("/setlist/" + setlistId)
                .retrieve()
                .body(Setlist.class);
    }

}
