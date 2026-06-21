package org.hagelbrand.controller;

import jakarta.servlet.http.HttpSession;
import org.hagelbrand.config.OAuth2LoginSuccessHandler;
import org.hagelbrand.data.PlaylistMetadata;
import org.hagelbrand.data.SavePlaylistRequest;
import org.hagelbrand.data.UpdatePlaylistRequest;
import org.hagelbrand.service.AppUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/playlists")
public class UserPlaylistController {

    private final AppUserService appUserService;

    public UserPlaylistController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    /** Save a newly created Spotify playlist to the user's library. */
    @PostMapping
    public PlaylistMetadata savePlaylist(
            HttpSession session,
            @RequestBody SavePlaylistRequest request
    ) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.savePlaylist(
                appUserId,
                "spotify",
                request.providerPlaylistId(),
                request.customName(),
                request.description(),
                request.sourceType(),
                request.trackIds()
        );
    }

    /** List all playlists in the user's library. */
    @GetMapping
    public List<PlaylistMetadata> getPlaylists(HttpSession session) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.getPlaylists(appUserId);
    }

    /** Get a single playlist by ID. */
    @GetMapping("/{playlistId}")
    public PlaylistMetadata getPlaylist(HttpSession session, @PathVariable UUID playlistId) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.getPlaylist(appUserId, playlistId);
    }

    /** Update a playlist's name or description. */
    @PutMapping("/{playlistId}")
    public PlaylistMetadata updatePlaylist(
            HttpSession session,
            @PathVariable UUID playlistId,
            @RequestBody UpdatePlaylistRequest request
    ) {
        UUID appUserId = resolveAppUserId(session);
        return appUserService.updatePlaylist(appUserId, playlistId, request.customName(), request.description());
    }

    /** Remove a playlist from the library (does not delete from Spotify). */
    @DeleteMapping("/{playlistId}")
    public void deletePlaylist(HttpSession session, @PathVariable UUID playlistId) {
        UUID appUserId = resolveAppUserId(session);
        appUserService.deletePlaylist(appUserId, playlistId);
    }

    private UUID resolveAppUserId(HttpSession session) {
        String id = (String) session.getAttribute(OAuth2LoginSuccessHandler.SESSION_APP_USER_ID);
        if (id == null) throw new IllegalStateException("Not authenticated");
        return UUID.fromString(id);
    }
}
