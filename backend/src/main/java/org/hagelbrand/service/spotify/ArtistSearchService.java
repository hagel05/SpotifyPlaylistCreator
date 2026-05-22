package org.hagelbrand.service.spotify;

import org.hagelbrand.data.ArtistSuggestion;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

import java.util.List;

public interface ArtistSearchService {
    List<ArtistSuggestion> searchArtists(String query, OAuth2AuthorizedClient client);
}
