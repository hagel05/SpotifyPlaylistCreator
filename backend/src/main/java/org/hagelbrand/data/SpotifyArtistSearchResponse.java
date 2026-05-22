package org.hagelbrand.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyArtistSearchResponse(Artists artists) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artists(List<Item> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(String id, String name, List<Image> images) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(String url, int height, int width) {}
}
