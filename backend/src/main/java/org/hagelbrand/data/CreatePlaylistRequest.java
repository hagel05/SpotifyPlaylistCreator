package org.hagelbrand.data;

public record CreatePlaylistRequest(
        String name,
        String description,
        boolean isPublic
) {}
