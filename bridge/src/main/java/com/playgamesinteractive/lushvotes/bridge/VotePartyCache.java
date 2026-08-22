package com.playgamesinteractive.lushvotes.bridge;

/**
 * Local mirror of vote party progress, pushed down after every credited
 * vote network-wide (not just ones that happened on this backend) - see
 * VotesBridgeListener's {@code pushProgress}. Backs
 * %lushvotes_party_current%/%lushvotes_party_target%/%lushvotes_party_remaining%.
 */
final class VotePartyCache {

    private volatile int current = 0;
    private volatile int target = 0;

    void update(int current, int target) {
        this.current = current;
        this.target = target;
    }

    int current() {
        return current;
    }

    int target() {
        return target;
    }

    int remaining() {
        return Math.max(0, target - current);
    }
}
