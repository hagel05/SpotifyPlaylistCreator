package org.hagelbrand.controller;

import org.hagelbrand.data.ArtistSuggestion;
import org.hagelbrand.service.spotify.ArtistSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistSearchController {

    private static final Logger log = LoggerFactory.getLogger(ArtistSearchController.class);

    private final ArtistSearchService artistSearchService;

    public ArtistSearchController(ArtistSearchService artistSearchService) {
        this.artistSearchService = artistSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ArtistSuggestion>> searchArtists(
            @RequestParam String q,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient spotifyClient
    ) {
        if (q == null || q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        log.info("Artist search request for query: {}", q);
        return ResponseEntity.ok(artistSearchService.searchArtists(q, spotifyClient));
    }
}
