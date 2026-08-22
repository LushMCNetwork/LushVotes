package com.playgamesinteractive.lushvotes.storage;

import java.time.Instant;
import java.util.UUID;

public record PendingReward(long id, UUID uuid, String username, String service,
                             Instant createdAt, Instant deliveredAt, RewardKind kind) {

    public boolean delivered() {
        return deliveredAt != null;
    }
}
