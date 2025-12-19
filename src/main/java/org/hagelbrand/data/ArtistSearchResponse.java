package org.hagelbrand.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArtistSearchResponse(
        String type,
        int itemsPerPage,
        int page,
        int total,


        @JsonProperty("artist")
        List<Artist> artists
) {


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(
            String mbid,
            String name,
            String sortName,
            String disambiguation,
            String url
    ) {}
}