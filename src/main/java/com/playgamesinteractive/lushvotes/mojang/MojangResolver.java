package com.playgamesinteractive.lushvotes.mojang;

import com.playgamesinteractive.lushvotes.storage.CachedUuid;
import com.playgamesinteractive.lushvotes.storage.UuidCacheRepository;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Username -> UUID resolution used by every credited vote (a name change
 * means the wrong account gets credited if this is skipped). Cache-first;
 * a live Mojang call only happens on a cache miss or an expired entry, and
 * re-resolves every 30 days to tolerate name changes without hammering
 * Mojang for players who vote often.
 * <p>
 * v1 makes exactly one lookup attempt per vote - no persistent retry queue.
 * A Mojang failure logs a warning and the vote is not credited; recovering
 * it is what {@code /lushvotes admin credit} is for.
 */
public final class MojangResolver {

    private static final Duration CACHE_TTL = Duration.ofDays(30);

    private final MojangLookup lookup;
    private final UuidCacheRepository cache;
    private final Clock clock;
    private final Logger logger;

    public MojangResolver(MojangLookup lookup, UuidCacheRepository cache, Clock clock, Logger logger) {
        this.lookup = lookup;
        this.cache = cache;
        this.clock = clock;
        this.logger = logger;
    }

    public CompletableFuture<Optional<UUID>> resolve(String username) {
        Optional<CachedUuid> cached = cache.find(username);
        if (cached.isPresent() && !isExpired(cached.get())) {
            return CompletableFuture.completedFuture(Optional.of(cached.get().uuid()));
        }
        return lookup.lookup(username).thenApply(result -> {
            if (result.isPresent()) {
                cache.put(username, result.get(), clock.instant());
            } else {
                logger.warn("Could not resolve a UUID for '{}' - vote will not be credited this attempt", username);
            }
            return result;
        });
    }

    private boolean isExpired(CachedUuid cached) {
        return Instant.now(clock).isAfter(cached.resolvedAt().plus(CACHE_TTL));
    }
}
