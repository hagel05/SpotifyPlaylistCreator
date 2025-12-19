package org.hagelbrand.service.spotify;

import org.hagelbrand.data.TrackCount;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpotifyPlaylistOrchestrator {

    private final SpotifyTrackResolver resolver;
    private final SpotifyPlaylistService playlistService;
    private final SpotifyUserService userService;

    public SpotifyPlaylistOrchestrator(
            SpotifyTrackResolver resolver,
            SpotifyPlaylistService playlistService,
            SpotifyUserService userService) {
        this.resolver = resolver;
        this.playlistService = playlistService;
        this.userService = userService;
    }

    public String buildPlaylist(
            OAuth2AuthorizedClient spotifyClient,
            String artist,
            List<TrackCount> tracks) {

        // Uses logged-in user's token
        String userId = userService.getCurrentUserId(spotifyClient);

        List<String> trackIds = tracks.stream()
                .map(tc -> resolver.resolveTrackId(artist, tc.track()))
                .flatMap(Optional::stream)
                .toList();

        String playlistId =
                playlistService.createPlaylist(
                        spotifyClient,
                        userId,
                        artist
                );

        playlistService.addTracks(
                spotifyClient,
                playlistId,
                trackIds
        );

        return playlistId;
    }
}
