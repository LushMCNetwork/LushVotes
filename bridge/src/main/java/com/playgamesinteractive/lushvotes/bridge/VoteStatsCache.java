package com.playgamesinteractive.lushvotes.bridge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player vote totals mirrored from the proxy, backing
 * %lushvotes_total%/%lushvotes_last%. Only ever holds entries for players
 * who've been on this backend since it started - cleared on quit so it
 * can't answer stale placeholders for someone who isn't here anymore.
 */
final class VoteStatsCache {

    record Stats(long totalVotes, long lastVoteAtEpochMillis) {
        static final Stats EMPTY = new Stats(0L, -1L);
    }

    private final Map<UUID, Stats> stats = new ConcurrentHashMap<>();

    void update(UUID uuid, long totalVotes, long lastVoteAtEpochMillis) {
        stats.put(uuid, new Stats(totalVotes, lastVoteAtEpochMillis));
    }

    void clear(UUID uuid) {
        stats.remove(uuid);
    }

    Stats get(UUID uuid) {
        return stats.getOrDefault(uuid, Stats.EMPTY);
    }
}
