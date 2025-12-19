package org.hagelbrand.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.hagelbrand.codec.StateDeserializer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SetlistSearchResponse(
        @JsonProperty("setlist")
        List<Setlist> setlists,
        int total,
        int page,
        int itemsPerPage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Setlist(
            Artist artist,
            Venue venue,
            Tour tour,
            Sets sets,
            String info,
            String url,
            String id,
            String versionId,
            String eventDate,
            String lastUpdated
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artist(
            String mbid,
            String name,
            String sortName,
            String disambiguation,
            String url
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Venue(
            City city,
            String url,
            String id,
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(
            String id,
            String name,
            @JsonDeserialize(using = StateDeserializer.class)
            State state,
            String stateCode,
            Country country
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record State(
            String name,
            String code
    ) {
        public static State fromString(String value) {
            return new State(value, null);
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Country(
            String name,
            String code
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tour(
            String name
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Set(
            String name,
            Integer encore,
            List<Song> song
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sets(
            @JsonProperty("set")
            List<Set> set
    ) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Song(
            String name,
            Boolean tape,
            String info
    ) {}
}
