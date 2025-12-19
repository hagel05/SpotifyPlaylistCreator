package org.hagelbrand.service.spotify;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

public interface SpotifyUserService {
    String getCurrentUserId(OAuth2AuthorizedClient client);
}
