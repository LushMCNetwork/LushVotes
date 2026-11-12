package com.playgamesinteractive.lushvotes.bridge;

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
