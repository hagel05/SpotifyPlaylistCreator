package org.hagelbrand.controller;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.hagelbrand.service.spotify.SpotifyPlaylistOrchestrator;
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

    public PlaylistController(ArtistSetlistPredictorService artistSetlistPredictorService,
                              SpotifyPlaylistOrchestrator spotifyPlaylistOrchestrator) {
        this.artistSetlistPredictorService = artistSetlistPredictorService;
        this.spotifyPlaylistOrchestrator = spotifyPlaylistOrchestrator;
    }

    @GetMapping("/{artist}/spotify-playlist")
    public Map<String, String> createPlaylist(
            @PathVariable String artist,
            @RequestParam(defaultValue = "20") int limit,
            @RegisteredOAuth2AuthorizedClient("spotify") OAuth2AuthorizedClient spotifyClient
    ) {
        log.info("Creating playlist for artist {}", artist);
        List<TrackCount> tracks = artistSetlistPredictorService.getTopTracksForArtist(artist, limit);
        log.info("Top tracks for artist {} are {}", artist, tracks);
        String playlistId = spotifyPlaylistOrchestrator.buildPlaylist(spotifyClient, artist, tracks);

        // TODO: Make this an object and include the setlistFM tracks as well as info about what we added to spotify
        return Map.of(
                "playlistId", playlistId,
                "url", "https://open.spotify.com/playlist/" + playlistId
        );
    }
}
