package org.hagelbrand.service.spotify;

import org.hagelbrand.data.SpotifySearchResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SpotifyServiceImpl {

    private final WebClient webClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public SpotifyServiceImpl(WebClient webClient,
                              OAuth2AuthorizedClientService authorizedClientService) {
        this.webClient = webClient;
        this.authorizedClientService = authorizedClientService;
    }

    public SpotifySearchResponse searchTrack(String artist, String track) {
        String q = String.format("track:%s artist:%s", track, artist);

        // Get currently authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            throw new IllegalStateException("No logged-in OAuth2 user");
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauth2Token.getAuthorizedClientRegistrationId(),
                oauth2Token.getName()
        );

        String accessToken = client.getAccessToken().getTokenValue();

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", q)
                        .queryParam("type", "track")
                        .queryParam("limit", 1)
                        .build())
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(SpotifySearchResponse.class)
                .block(); // blocking for simplicity in service; can refactor to reactive if desired
    }
}
