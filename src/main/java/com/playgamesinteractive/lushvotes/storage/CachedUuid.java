package com.playgamesinteractive.lushvotes.storage;

import java.time.Instant;
import java.util.UUID;

public record CachedUuid(UUID uuid, Instant resolvedAt) {
}
