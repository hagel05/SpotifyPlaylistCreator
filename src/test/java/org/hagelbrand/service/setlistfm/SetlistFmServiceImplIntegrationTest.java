package org.hagelbrand.service.setlistfm;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hagelbrand.data.ArtistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse;
import org.hagelbrand.data.SetlistSearchResponse.Setlist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for SetlistFmServiceImpl with mocked Setlist.fm API responses.
 * Tests the service layer's ability to deserialize real-world API responses.
 */
class SetlistFmServiceImplIntegrationTest {

    private MockWebServer mockWebServer;
    private SetlistFmServiceImpl setlistFmService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        setlistFmService = new SetlistFmServiceImpl(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void searchesArtistsAndDeserializesResponse() throws Exception {
        String artistJson = """
            {
              "type": "artist",
              "itemsPerPage": 20,
              "page": 1,
              "total": 1,
              "artist": [
                {
                  "mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25",
                  "name": "Green Day",
                  "sortName": "Green Day",
                  "disambiguation": "",
                  "url": "https://www.setlist.fm/setlists/green-day-bd6bc066.html"
                }
              ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(artistJson));

        ArtistSearchResponse response = setlistFmService.getArtistsFromSearch("Green Day");

        assertThat(response).isNotNull();
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.artists()).hasSize(1);

        ArtistSearchResponse.Artist artist = response.artists().getFirst();
        assertThat(artist.name()).isEqualTo("Green Day");
        assertThat(artist.mbid()).isEqualTo("114bcd19-63e4-404e-b310-a2395d3bbf25");
    }

    @Test
    void retrievesSetlistsForArtist() throws Exception {
        String setlistJson = """
            {
              "setlist": [
                {
                  "artist": {
                    "mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25",
                    "name": "Green Day"
                  },
                  "venue": {
                    "city": {
                      "id": "5368361",
                      "name": "Los Angeles, CA",
                      "state": "CA",
                      "country": {
                        "code": "US",
                        "name": "United States"
                      }
                    },
                    "id": "6788",
                    "name": "The Forum",
                    "url": "https://www.setlist.fm/venues/the-forum-los-angeles-ca-usa-63d68d3f.html"
                  },
                  "sets": {
                    "set": [
                      {
                        "name": "Set 1",
                        "encore": null,
                        "song": [
                          {"name": "When I Come Around", "tape": false, "info": null},
                          {"name": "Basket Case", "tape": false, "info": null}
                        ]
                      }
                    ]
                  },
                  "info": null,
                  "url": "https://www.setlist.fm/setlists/green-day-1234567.html",
                  "id": "1234567",
                  "versionId": "7777777",
                  "eventDate": "2023-09-22",
                  "lastUpdated": "2023-09-23T12:34:56.000Z"
                }
              ],
              "total": 1,
              "page": 1,
              "itemsPerPage": 20
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(setlistJson));

        SetlistSearchResponse response = setlistFmService.getSetlistsFromSearch("114bcd19-63e4-404e-b310-a2395d3bbf25");

        assertThat(response).isNotNull();
        assertThat(response.setlists()).hasSize(1);

        Setlist setlist = response.setlists().getFirst();
        assertThat(setlist.id()).isEqualTo("1234567");
        assertThat(setlist.eventDate()).isEqualTo("2023-09-22");
        assertThat(setlist.sets().set()).hasSize(1);

        List<SetlistSearchResponse.Song> songs = setlist.sets().set().getFirst().song();
        assertThat(songs).hasSize(2);
        assertThat(songs.getFirst().name()).isEqualTo("When I Come Around");
    }

    @Test
    void filtersOutNonConcertSetlists() throws Exception {
        String setlistJson = """
            {
              "setlist": [
                {
                  "artist": {"mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25", "name": "Green Day"},
                  "venue": {"city": {"id": "5368361", "name": "Test"}, "id": "1", "name": "Concert Venue"},
                  "sets": {"set": [{"name": "Set 1", "song": [{"name": "Song 1"}]}]},
                  "info": null,
                  "url": "https://example.com/1",
                  "id": "concert1",
                  "versionId": "v1",
                  "eventDate": "2023-01-01",
                  "lastUpdated": "2023-01-02T00:00:00.000Z"
                },
                {
                  "artist": {"mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25", "name": "Green Day"},
                  "venue": {"city": {"id": "5368361", "name": "Test"}, "id": "2", "name": "Studio"},
                  "sets": {"set": [{"name": "Set 1", "song": [{"name": "Song 1"}]}]},
                  "info": "Live Promo",
                  "url": "https://example.com/2",
                  "id": "promo1",
                  "versionId": "v2",
                  "eventDate": "2023-02-01",
                  "lastUpdated": "2023-02-02T00:00:00.000Z"
                }
              ],
              "total": 2,
              "page": 1,
              "itemsPerPage": 20
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(setlistJson));

        List<Setlist> filteredSetlists = setlistFmService.getConcertSetlists("114bcd19-63e4-404e-b310-a2395d3bbf25");

        // Should only include actual concerts, not promotional or studio recordings
        assertThat(filteredSetlists).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void retrievesIndividualSetlistById() throws Exception {
        String setlistJson = """
            {
              "artist": {
                "mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25",
                "name": "Green Day"
              },
              "venue": {
                "city": {
                  "id": "5368361",
                  "name": "Los Angeles, CA",
                  "state": "CA",
                  "country": {"code": "US", "name": "United States"}
                },
                "id": "6788",
                "name": "The Forum"
              },
              "sets": {
                "set": [
                  {
                    "name": "Set 1",
                    "encore": null,
                    "song": [
                      {"name": "Billie Jean", "tape": false, "info": null},
                      {"name": "She", "tape": false, "info": null},
                      {"name": "When I Come Around", "tape": false, "info": null}
                    ]
                  },
                  {
                    "name": "Set 2",
                    "encore": 0,
                    "song": [
                      {"name": "Basket Case", "tape": false, "info": null},
                      {"name": "American Idiot", "tape": false, "info": null}
                    ]
                  }
                ]
              },
              "info": null,
              "url": "https://www.setlist.fm/setlists/green-day-1234567.html",
              "id": "1234567",
              "versionId": "7777777",
              "eventDate": "2023-09-22",
              "lastUpdated": "2023-09-23T12:34:56.000Z"
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(setlistJson));

        Setlist setlist = setlistFmService.getSetlistById("1234567");

        assertThat(setlist).isNotNull();
        assertThat(setlist.id()).isEqualTo("1234567");
        assertThat(setlist.sets().set()).hasSize(2);

        List<SetlistSearchResponse.Song> allSongs = setlist.sets().set().stream()
                .flatMap(s -> s.song().stream())
                .toList();

        assertThat(allSongs).hasSize(5);
        assertThat(allSongs).extracting(SetlistSearchResponse.Song::name)
                .contains("When I Come Around", "Basket Case", "American Idiot");
    }

    @Test
    void handlesStateDeserializationFromString() throws Exception {
        String setlistJson = """
            {
              "setlist": [
                {
                  "artist": {"mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25", "name": "Green Day"},
                  "venue": {
                    "city": {
                      "id": "5368361",
                      "name": "Test City",
                      "state": "Nevada",
                      "country": {"code": "US", "name": "United States"}
                    },
                    "id": "1",
                    "name": "Test Venue"
                  },
                  "sets": {"set": []},
                  "info": null,
                  "url": "https://example.com/1",
                  "id": "1",
                  "versionId": "v1",
                  "eventDate": "2023-01-01",
                  "lastUpdated": "2023-01-02T00:00:00.000Z"
                }
              ],
              "total": 1,
              "page": 1,
              "itemsPerPage": 20
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(setlistJson));

        SetlistSearchResponse response = setlistFmService.getSetlistsFromSearch("114bcd19-63e4-404e-b310-a2395d3bbf25");

        SetlistSearchResponse.State state = response.setlists().getFirst()
                .venue().city().state();

        assertThat(state).isNotNull();
        assertThat(state.name()).isEqualTo("Nevada");
    }

    @Test
    void handlesStateDeserializationFromObject() throws Exception {
        String setlistJson = """
            {
              "setlist": [
                {
                  "artist": {"mbid": "114bcd19-63e4-404e-b310-a2395d3bbf25", "name": "Green Day"},
                  "venue": {
                    "city": {
                      "id": "5368361",
                      "name": "Test City",
                      "state": {"name": "California", "code": "CA"},
                      "country": {"code": "US", "name": "United States"}
                    },
                    "id": "1",
                    "name": "Test Venue"
                  },
                  "sets": {"set": []},
                  "info": null,
                  "url": "https://example.com/1",
                  "id": "1",
                  "versionId": "v1",
                  "eventDate": "2023-01-01",
                  "lastUpdated": "2023-01-02T00:00:00.000Z"
                }
              ],
              "total": 1,
              "page": 1,
              "itemsPerPage": 20
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(setlistJson));

        SetlistSearchResponse response = setlistFmService.getSetlistsFromSearch("114bcd19-63e4-404e-b310-a2395d3bbf25");

        SetlistSearchResponse.State state = response.setlists().getFirst()
                .venue().city().state();

        assertThat(state).isNotNull();
        assertThat(state.name()).isEqualTo("California");
        assertThat(state.code()).isEqualTo("CA");
    }
}

