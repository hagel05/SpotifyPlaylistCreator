package org.hagelbrand.controller;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.service.setlistfm.ArtistSetlistPredictorService;
import org.hagelbrand.service.spotify.SpotifyPlaylistOrchestrator;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

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
        List<TrackCount> tracks = artistSetlistPredictorService.getTopTracksForArtist(artist, limit);
        String playlistId = spotifyPlaylistOrchestrator.buildPlaylist(spotifyClient, artist, tracks);

        return Map.of(
                "playlistId", playlistId,
                "url", "https://open.spotify.com/playlist/" + playlistId
        );
    }



}
