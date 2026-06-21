package org.hagelbrand.data;

/**
 * Request body for updating a playlist's name or description.
 */
public record UpdatePlaylistRequest(
        String customName,
        String description
) {}
