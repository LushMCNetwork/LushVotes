package com.playgamesinteractive.lushvotes.mojang;

import com.playgamesinteractive.lushvotes.storage.Database;
import com.playgamesinteractive.lushvotes.storage.UuidCacheRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojangResolverTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MojangResolverTest.class);
    private static final UUID STEVE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Database database;
    private UuidCacheRepository cache;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        database = new Database(tempDir, LOGGER);
        database.open();
        cache = new UuidCacheRepository(database, LOGGER);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void cacheMissCallsTheLookupAndCachesTheResult() throws Exception {
        AtomicInteger lookups = new AtomicInteger();
        MojangLookup lookup = username -> {
            lookups.incrementAndGet();
            return CompletableFuture.completedFuture(Optional.of(STEVE));
        };
        MojangResolver resolver = new MojangResolver(lookup, cache, Clock.systemUTC(), LOGGER);

        Optional<UUID> result = resolver.resolve("Steve").get();

        assertEquals(Optional.of(STEVE), result);
        assertEquals(1, lookups.get());
        assertEquals(STEVE, cache.find("Steve").orElseThrow().uuid());
    }

    @Test
    void cacheHitNeverCallsTheLookup() throws Exception {
        cache.put("Steve", STEVE, Instant.now());
        AtomicInteger lookups = new AtomicInteger();
        MojangLookup lookup = username -> {
            lookups.incrementAndGet();
            return CompletableFuture.completedFuture(Optional.empty());
        };
        MojangResolver resolver = new MojangResolver(lookup, cache, Clock.systemUTC(), LOGGER);

        Optional<UUID> result = resolver.resolve("Steve").get();

        assertEquals(Optional.of(STEVE), result);
        assertEquals(0, lookups.get());
    }

    @Test
    void expiredCacheEntryTriggersAFreshLookup() throws Exception {
        Instant longAgo = Instant.parse("2020-01-01T00:00:00Z");
        cache.put("Steve", STEVE, longAgo);
        AtomicInteger lookups = new AtomicInteger();
        UUID renamed = UUID.fromString("22222222-2222-2222-2222-222222222222");
        MojangLookup lookup = username -> {
            lookups.incrementAndGet();
            return CompletableFuture.completedFuture(Optional.of(renamed));
        };
        Clock now = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC); // 30+ days past the cache TTL
        MojangResolver resolver = new MojangResolver(lookup, cache, now, LOGGER);

        Optional<UUID> result = resolver.resolve("Steve").get();

        assertTrue(lookups.get() > 0);
        assertEquals(Optional.of(renamed), result);
    }

    @Test
    void failedLookupIsNotCached() throws Exception {
        MojangLookup lookup = username -> CompletableFuture.completedFuture(Optional.empty());
        MojangResolver resolver = new MojangResolver(lookup, cache, Clock.systemUTC(), LOGGER);

        Optional<UUID> result = resolver.resolve("GhostPlayer").get();

        assertEquals(Optional.empty(), result);
        assertEquals(Optional.empty(), cache.find("GhostPlayer"));
    }
}
