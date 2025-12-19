package org.hagelbrand.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;


@Configuration
public class SetlistFmClientConfig {

    @Value("${setlistfm.api-token}")
    private String setlistFmApiToken;

    @Bean
    public RestClient setlistFmClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://api.setlist.fm/rest/1.0")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader("x-api-key", setlistFmApiToken)
                .build();
    }
}