package org.hagelbrand.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
public class SpotifyController {

    private final RestClient restClient = RestClient.create();

    @GetMapping("/api/auth/check")
    public Map<String, Object> checkAuth(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return Map.of(
                    "authenticated", true,
                    "principal", authentication.getPrincipal().toString()
            );
        }
        return Map.of("authenticated", false);
    }

    @GetMapping("/api/spotify/me")
    public Map<String, Object> me(
            @RegisteredOAuth2AuthorizedClient("spotify")
            OAuth2AuthorizedClient client
    ) {
        String accessToken = client.getAccessToken().getTokenValue();

        return restClient.get()
                .uri("https://api.spotify.com/v1/me")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);
    }
}
