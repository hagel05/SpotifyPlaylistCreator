package org.hagelbrand.service.spotify;

import org.hagelbrand.data.ArtistSuggestion;
import org.hagelbrand.data.SpotifyArtistSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Service
public class ArtistSearchServiceImpl implements ArtistSearchService {

    private static final Logger log = LoggerFactory.getLogger(ArtistSearchServiceImpl.class);
    private static final int LIMIT = 8;

    private final WebClient webClient;

    public ArtistSearchServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public List<ArtistSuggestion> searchArtists(String query, OAuth2AuthorizedClient client) {
        log.info("Searching Spotify artists for query: {}", query);
        String accessToken = client.getAccessToken().getTokenValue();

        SpotifyArtistSearchResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", query)
                        .queryParam("type", "artist")
                        .queryParam("limit", LIMIT)
                        .build())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(SpotifyArtistSearchResponse.class)
                .block();

        if (response == null || response.artists() == null || response.artists().items() == null) {
            return Collections.emptyList();
        }

        return response.artists().items().stream()
                .map(item -> new ArtistSuggestion(
                        item.id(),
                        item.name(),
                        item.images() == null || item.images().isEmpty()
                                ? null
                                : item.images().get(0).url()
                ))
                .toList();
    }
}
