package org.hagelbrand.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifySearchResponse(Tracks tracks) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tracks(List<Item> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String id,
            String name,
            List<Artist> artists,
            Album album,
            int popularity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Album(String id,
                        String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(String name) {}
}
