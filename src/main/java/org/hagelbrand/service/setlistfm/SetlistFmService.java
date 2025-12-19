package org.hagelbrand.service.setlistfm;

import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;

import java.util.List;

public interface SetlistFmService {
    ArtistSearchResponse getArtistsFromSearch(String artistName);

    SetlistSearchResponse getSetlistsFromSearch(String mbid);

    List<Setlist> getConcertSetlists(String mbid);

    Setlist getSetlistById(String setlistId);

    }
