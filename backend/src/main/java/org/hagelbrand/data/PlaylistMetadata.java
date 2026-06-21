package org.hagelbrand.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistMetadata(
        UUID playlistId,
        String provider,
        String providerPlaylistId,
        String customName,
        String description,
        String sourceType,
        List<String> trackIds,
        Instant createdAt,
        Instant updatedAt
) {}
