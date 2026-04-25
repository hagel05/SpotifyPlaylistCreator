package org.hagelbrand.controller;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.hagelbrand.service.spotify.SpotifyPlaylistOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private static final Logger log = LoggerFactory.getLogger(PlaylistController.class);


    private final ArtistSetlistPredictorService artistSetlistPredictorService;
    private final SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator;

    public PlaylistController(ArtistSetlistPredictorService artistSetlistPredictorService,
                              SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator) {
        this.artistSetlistPredictorService = artistSetlistPredictorService;
        this.spotifyPlaylistOrchestrator = spotifyPlaylistOrchestrator;
    }

    @GetMapping("/{artist}/spotify-playlist")
    public Map<String, Object> createPlaylist(
            @PathVariable String artist,
            @RequestParam(defaultValue = "20") int limit,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient spotifyClient
    ) {
        log.info("Creating playlist for artist {}", artist);
        List<TrackCount> tracks = artistSetlistPredictorService.getTopTracksForArtist(artist, limit)
                .stream()
                .sorted(Comparator.comparingLong(TrackCount::plays))
                .toList();

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
}
