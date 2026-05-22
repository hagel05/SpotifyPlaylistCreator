package org.hagelbrand.data;

/**
 * The result of resolving one setlist.fm track name to a Spotify track.
 *
 * @param setlistTrack      The raw track name from setlist.fm.
 * @param artist            The artist used for the Spotify search (may be the cover's
 *                          original artist rather than the performing artist).
 * @param matched           Whether a usable Spotify match was found.
 * @param spotifyTrackId    Spotify track ID, or {@code null} when not matched.
 * @param spotifyTrackName  Track name as Spotify returned it, or {@code null} when not matched.
 * @param spotifyArtistName Primary artist name from Spotify, or {@code null} when not matched.
 * @param confidence        Composite confidence score (higher is better).
 * @param reason            Human-readable reason string used for logging/debugging.
 * @param albumImageUrl     URL of the album thumbnail image, or {@code null} when not matched.
 */
public record TrackResolution(
        String setlistTrack,
        String artist,
        boolean matched,
        String spotifyTrackId,
        String spotifyTrackName,
        String spotifyArtistName,
        int confidence,
        String reason,
        String albumImageUrl
) {}
