package org.hagelbrand.data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppUser(
        UUID appUserId,
        String displayName,
        List<ProviderAccount> linkedProviders,
        Instant createdAt
) {}
