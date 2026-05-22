package org.hagelbrand.controller;

import org.hagelbrand.data.CreatePlaylistFromTracksRequest;
import org.hagelbrand.data.TrackCount;
import org.hagelbrand.data.TrackPreview;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.hagelbrand.service.spotify.SpotifyPlaylistOrchestrator;
import org.hagelbrand.service.spotify.SpotifyPlaylistService;
import org.hagelbrand.service.spotify.SpotifyTrackResolver;
import org.hagelbrand.service.spotify.SpotifyUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);

    private final ArtistSetlistPredictorService artistSetlistPredictorService;
    private final SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator;
    private final SpotifyTrackResolver trackResolver;
    private final SpotifyPlaylistService playlistService;
    private final SpotifyUserService userService;

    public PlaylistController(ArtistSetlistPredictorService artistSetlistPredictorService,
                              SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator,
                              SpotifyTrackResolver trackResolver,
                              SpotifyPlaylistService playlistService,
                              SpotifyUserService userService) {
        this.artistSetlistPredictorService = artistSetlistPredictorService;
        this.spotifyPlaylistOrchestrator = spotifyPlaylistOrchestrator;
        this.trackResolver = trackResolver;
        this.playlistService = playlistService;
        this.userService = userService;
    }

    // ── Existing endpoint (kept for backward compatibility) ───────────────────

    @GetMapping("/{artist}/spotify-playlist")
    public Map<String, Object> createPlaylist(
            @PathVariable String artist,
            @RequestParam(defaultValue = "20") int limit,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient spotifyClient
    ) {
        log.info("Creating playlist for artist {}", artist);
        List<TrackCount> tracks = artistSetlistPredictorService.getTopTracksForArtist(artist, limit);

        log.info("Top tracks for artist {} are {}", artist, tracks);
        String playlistId = spotifyPlaylistOrchestrator.buildPlaylist(spotifyClient, artist, tracks);

        if (playlistId == null || playlistId.isEmpty()) {
            playlistId = "unknown";
        }

        return Map.of(
                "playlistId", playlistId,
                "playlistUrl", "https://open.spotify.com/playlist/" + playlistId,
                "artist", artist,
                "tracksAdded", tracks.size(),
                "topTracks", tracks.stream()
                        .map(t -> Map.of("name", t.track(), "confidence", 0.85))
                        .toList(),
                "createdAt", System.currentTimeMillis()
        );
    }

    // ── New two-phase flow ────────────────────────────────────────────────────

    /**
     * Phase 1 — resolve the top tracks for {@code artist} against Spotify and return
     * the full preview (best match + alternatives) without creating any playlist.
     * The frontend uses this to let the user deselect or swap tracks before committing.
     */
    @GetMapping("/{artist}/preview")
    public List<TrackPreview> previewPlaylist(
            @PathVariable String artist,
            @RequestParam(defaultValue = "20") int limit,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient spotifyClient
    ) {
        log.info("Previewing playlist for artist {}", artist);
        List<TrackCount> tracks = artistSetlistPredictorService.getTopTracksForArtist(artist, limit);

        return tracks.stream()
                .map(tc -> trackResolver.resolveWithAlternatives(artist, tc))
                .toList();
    }

    /**
     * Phase 2 — create a Spotify playlist from a pre-confirmed list of track IDs.
     * The caller (frontend) has already resolved, reviewed, and possibly swapped
     * tracks in Phase 1; this endpoint just creates the playlist without re-resolving.
     */
    @PostMapping("/{artist}/create-from-tracks")
    public Map<String, Object> createPlaylistFromTracks(
            @PathVariable String artist,
            @RequestBody CreatePlaylistFromTracksRequest request,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient spotifyClient
    ) {
        log.info("Creating playlist for artist {} from {} pre-resolved tracks",
                artist, request.trackIds().size());

        String userId = userService.getCurrentUserId(spotifyClient);
        String playlistId = playlistService.createPlaylist(spotifyClient, userId, artist);
        playlistService.addTracks(spotifyClient, playlistId, request.trackIds());

        return Map.of(
                "playlistId", playlistId,
                "playlistUrl", "https://open.spotify.com/playlist/" + playlistId,
                "artist", artist,
                "tracksAdded", request.trackIds().size(),
                "createdAt", System.currentTimeMillis()
        );
    }
}
