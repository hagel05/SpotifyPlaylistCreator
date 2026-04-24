package org.hagelbrand.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class SetlistFmClientConfigTest {

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
    void createsSetlistFmRestClientWithExpectedHeaders() throws InterruptedException {
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setBody("{}")
        );

        ApplicationContextRunner contextRunner =
                new ApplicationContextRunner()
                        .withConfiguration(
                                AutoConfigurations.of(
                                        RestClientAutoConfiguration.class
                                )
                        )
                        .withUserConfiguration(SetlistFmClientConfig.class)
                        .withPropertyValues(
                                "setlistfm.api-token=test-api-token"
                        );

        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RestClient.class);

            RestClient client = context.getBean(RestClient.class);

            client.get()
                    .uri(mockWebServer.url("/search").toString())
                    .retrieve()
                    .toBodilessEntity();

            var request = mockWebServer.takeRequest();

            assertThat(request.getHeader(HttpHeaders.ACCEPT))
                    .isEqualTo("application/json");

            assertThat(request.getHeader("x-api-key"))
                    .isEqualTo("test-api-token");
        });
    }
}
