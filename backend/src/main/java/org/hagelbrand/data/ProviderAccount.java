package org.hagelbrand.data;

import java.time.Instant;
import java.util.UUID;

public record ProviderAccount(
        UUID providerAccountId,
        String provider,
        String providerUserId,
        String providerEmail,
        Instant linkedAt
) {}
