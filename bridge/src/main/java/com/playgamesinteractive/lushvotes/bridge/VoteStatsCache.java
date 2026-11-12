package com.playgamesinteractive.lushvotes.bridge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
