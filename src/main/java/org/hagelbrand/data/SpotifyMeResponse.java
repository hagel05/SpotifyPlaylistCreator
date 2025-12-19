package org.hagelbrand.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyMeResponse(
        String id,
        String display_name
) {}
