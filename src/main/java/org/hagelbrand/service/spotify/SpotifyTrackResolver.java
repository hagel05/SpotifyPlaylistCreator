package org.hagelbrand.service.spotify;

import org.hagelbrand.data.SpotifySearchResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SpotifyTrackResolver {

    private final SpotifyServiceImpl spotifyService;

    public SpotifyTrackResolver(SpotifyServiceImpl spotifyService) {
        this.spotifyService = spotifyService;
    }

    public Optional<String> resolveTrackId(String artist, String track) {
        SpotifySearchResponse response = spotifyService.searchTrack(artist, track);

        if (response == null ||
                response.tracks() == null ||
                response.tracks().items().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(response.tracks().items().get(0).id());
    }
}
