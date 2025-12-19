package org.hagelbrand.service.spotify;

import org.hagelbrand.data.SpotifyMeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

@Service
public class SpotifyUserServiceImpl implements SpotifyUserService {

    private final WebClient spotifyWebClient;

    public SpotifyUserServiceImpl(WebClient spotifyWebClient) {
        this.spotifyWebClient = spotifyWebClient;
    }

    @Override
    public String getCurrentUserId(OAuth2AuthorizedClient client) {
        // Use the OAuth2AuthorizedClient token
        SpotifyMeResponse me = spotifyWebClient.get()
                .uri("/me")
                .headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue()))
                .retrieve()
                .bodyToMono(SpotifyMeResponse.class)
                .block(); // For simplicity; could also be reactive

        if (me == null || me.id() == null) {
            throw new IllegalStateException("Failed to resolve Spotify user identity");
        }

        return me.id();
    }
}
