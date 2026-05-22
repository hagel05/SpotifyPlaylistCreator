package org.hagelbrand.data;

import java.util.List;

/**
 * Request body for {@code POST /api/playlist/{artist}/create-from-tracks}.
 * Contains the pre-resolved Spotify track IDs the user confirmed after reviewing
 * the preview step (deselections and swaps already applied).
 */
public record CreatePlaylistFromTracksRequest(List<String> trackIds) {}
