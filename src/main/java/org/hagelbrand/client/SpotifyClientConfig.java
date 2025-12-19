package org.hagelbrand.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SpotifyClientConfig {

    @Bean
    WebClient spotifyWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();
    }
}
