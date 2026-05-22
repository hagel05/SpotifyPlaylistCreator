package org.hagelbrand.data;

/**
 * A Spotify track that could substitute for the primary resolution choice.
 * Shown in the frontend swap picker so users can override bad matches.
 */
public record AlternativeTrack(
        String spotifyTrackId,
        String spotifyTrackName,
        String spotifyArtistName,
        int confidence,
        String albumImageUrl
) {}
