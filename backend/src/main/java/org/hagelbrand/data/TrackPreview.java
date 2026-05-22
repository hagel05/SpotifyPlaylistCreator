package org.hagelbrand.data;

import java.util.List;

/**
 * Full resolution preview for one setlist.fm track, returned by the
 * {@code /api/playlist/{artist}/preview} endpoint.
 *
 * @param setlistTrack      Raw track name from setlist.fm.
 * @param plays             Times the track appeared across fetched setlists.
 * @param coverArtist       Original artist if this is a cover; {@code null} otherwise.
 * @param matched           Whether a usable Spotify match was found.
 * @param spotifyTrackId    Spotify track ID chosen as the best match; {@code null} if unmatched.
 * @param spotifyTrackName  Track name as returned by Spotify; {@code null} if unmatched.
 * @param spotifyArtistName Primary Spotify artist name; {@code null} if unmatched.
 * @param confidence        Confidence score of the chosen match.
 * @param reason            Debug string explaining why this match was chosen.
 * @param alternatives      Up to 4 other Spotify tracks that scored above zero —
 *                          shown in the swap picker.
 */
public record TrackPreview(
        String setlistTrack,
        long plays,
        String coverArtist,
        boolean matched,
        String spotifyTrackId,
        String spotifyTrackName,
        String spotifyArtistName,
        int confidence,
        String reason,
        List<AlternativeTrack> alternatives
) {}
