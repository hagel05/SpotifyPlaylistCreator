package org.hagelbrand.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SpotifyClientConfigTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void createsSpotifyWebClientWithExpectedBaseUrl() {
        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner()
                        .withUserConfiguration(SpotifyClientConfig.class);

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebClient.class);

            WebClient webClient = context.getBean(WebClient.class);

            // enqueue fake response
            mockWebServer.enqueue(
                    new MockResponse()
                            .setResponseCode(200)
                            .setBody("{}")
            );

            // Use mutate() to override base URL for interception
            WebClient testClient =
                    webClient.mutate()
                            .baseUrl(mockWebServer.url("/v1").toString())
                            .build();

            testClient.get()
                    .uri("/me")
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            var request = mockWebServer.takeRequest();

            assertThat(request.getPath())
                    .isEqualTo("/v1/me");
        });
    }
}
