package org.hagelbrand.data;

public record TrackResolution(
        String setlistTrack,
        String artist,
        boolean matched,
        String spotifyTrackId,
        String spotifyTrackName,
        int confidence,
        String reason
) {}

