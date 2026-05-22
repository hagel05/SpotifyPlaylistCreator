package org.hagelbrand.service.spotify;

import org.hagelbrand.data.ArtistSuggestion;
import org.hagelbrand.data.SpotifyArtistSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistSearchServiceImplTest {

    @Mock
    private WebClient webClient;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private ArtistSearchServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new ArtistSearchServiceImpl(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any(Consumer.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private OAuth2AuthorizedClient mockClient() {
        OAuth2AuthorizedClient client = mock(OAuth2AuthorizedClient.class);
        OAuth2AccessToken token = mock(OAuth2AccessToken.class);
        when(token.getTokenValue()).thenReturn("test-access-token");
        when(client.getAccessToken()).thenReturn(token);
        return client;
    }

    @Test
    void returnsMappedSuggestionsFromSpotifyResponse() {
        SpotifyArtistSearchResponse response = new SpotifyArtistSearchResponse(
                new SpotifyArtistSearchResponse.Artists(List.of(
                        new SpotifyArtistSearchResponse.Item("1", "The Beatles",
                                List.of(new SpotifyArtistSearchResponse.Image("https://img.example.com/beatles.jpg", 640, 640))),
                        new SpotifyArtistSearchResponse.Item("2", "Beatles Revival",
                                Collections.emptyList())
                ))
        );

        when(responseSpec.bodyToMono(SpotifyArtistSearchResponse.class)).thenReturn(Mono.just(response));

        List<ArtistSuggestion> result = service.searchArtists("Beatles", mockClient());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("1");
        assertThat(result.get(0).name()).isEqualTo("The Beatles");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://img.example.com/beatles.jpg");
        assertThat(result.get(1).id()).isEqualTo("2");
        assertThat(result.get(1).imageUrl()).isNull();
    }

    @Test
    void picksFirstImageWhichIsLargest() {
        SpotifyArtistSearchResponse response = new SpotifyArtistSearchResponse(
                new SpotifyArtistSearchResponse.Artists(List.of(
                        new SpotifyArtistSearchResponse.Item("1", "Artist",
                                List.of(
                                        new SpotifyArtistSearchResponse.Image("https://img.example.com/large.jpg", 640, 640),
                                        new SpotifyArtistSearchResponse.Image("https://img.example.com/medium.jpg", 300, 300),
                                        new SpotifyArtistSearchResponse.Image("https://img.example.com/small.jpg", 64, 64)
                                ))
                ))
        );

        when(responseSpec.bodyToMono(SpotifyArtistSearchResponse.class)).thenReturn(Mono.just(response));

        List<ArtistSuggestion> result = service.searchArtists("Artist", mockClient());

        assertThat(result.get(0).imageUrl()).isEqualTo("https://img.example.com/large.jpg");
    }

    @Test
    void returnsNullImageUrlWhenImagesListIsEmpty() {
        SpotifyArtistSearchResponse response = new SpotifyArtistSearchResponse(
                new SpotifyArtistSearchResponse.Artists(List.of(
                        new SpotifyArtistSearchResponse.Item("1", "Obscure Artist", Collections.emptyList())
                ))
        );

        when(responseSpec.bodyToMono(SpotifyArtistSearchResponse.class)).thenReturn(Mono.just(response));

        List<ArtistSuggestion> result = service.searchArtists("Obscure Artist", mockClient());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).imageUrl()).isNull();
    }

    @Test
    void returnsEmptyListWhenResponseIsNull() {
        when(responseSpec.bodyToMono(SpotifyArtistSearchResponse.class)).thenReturn(Mono.empty());

        List<ArtistSuggestion> result = service.searchArtists("Beatles", mockClient());

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyListWhenArtistsItemsIsNull() {
        SpotifyArtistSearchResponse response = new SpotifyArtistSearchResponse(
                new SpotifyArtistSearchResponse.Artists(null)
        );

        when(responseSpec.bodyToMono(SpotifyArtistSearchResponse.class)).thenReturn(Mono.just(response));

        List<ArtistSuggestion> result = service.searchArtists("Beatles", mockClient());

        assertThat(result).isEmpty();
    }
}
