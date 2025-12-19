package org.hagelbrand.service.spotify;

import org.hagelbrand.data.TrackCount;
import org.hagelbrand.data.TrackResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SpotifyPlaylistOrchestrator {

    private final SpotifyTrackResolver resolver;
    private final SpotifyPlaylistService playlistService;
    private final SpotifyUserService userService;

    private static final Logger log = LoggerFactory.getLogger(SpotifyPlaylistOrchestrator.class);


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
        log.info("Creating playlist for user {}", userId);

        List<TrackResolution> resolutions = tracks.stream()
                .map(tc -> resolver.resolveTrackId(artist, tc.track()))
                .toList();

        log.info("Setlist.fm → Spotify resolution for '{}':", artist);

        resolutions.forEach(r -> {
            if (r.matched()) {
                log.info(
                        "  '{}' → '{}' ({}), {}",
                        r.setlistTrack(),
                        r.spotifyTrackName(),
                        r.spotifyTrackId(),
                        r.reason()
                );
            } else {
                log.warn(
                        "  '{}' → NO MATCH",
                        r.setlistTrack()
                );
            }
        });


        List<String> trackIds = resolutions.stream()
                .filter(TrackResolution::matched)
                .map(TrackResolution::spotifyTrackId)
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
