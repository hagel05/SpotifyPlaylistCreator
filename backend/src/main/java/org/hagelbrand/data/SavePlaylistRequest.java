package org.hagelbrand.data;

import java.util.List;

/**
 * Request body for saving a playlist to the user's library after creation.
 */
public record SavePlaylistRequest(
        String providerPlaylistId,
        String customName,
        String description,
        String sourceType,
        List<String> trackIds
) {}
