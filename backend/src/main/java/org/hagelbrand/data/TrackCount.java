package org.hagelbrand.data;

/**
 * A track name with its setlist.fm play count and optional cover-artist attribution.
 *
 * @param track       The track name as it appears on setlist.fm.
 * @param plays       How many times the track appeared across the fetched setlists.
 * @param coverArtist The original recording artist if this is a cover (e.g. "Britney Spears"),
 *                    or {@code null} if the song is an original by the performing artist.
 *                    The resolver uses this to search Spotify under the correct artist.
 */
public record TrackCount(String track, long plays, String coverArtist) {

    /** Convenience constructor for tracks that are not covers. */
    public TrackCount(String track, long plays) {
        this(track, plays, null);
    }
}
