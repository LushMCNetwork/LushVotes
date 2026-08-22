package com.playgamesinteractive.lushvotes.vote;

/**
 * Fired once the vote party counter reaches its target. Implemented by the
 * bridge-channel layer: enumerates every currently online player
 * network-wide, queues a PARTY-kind pending reward for each, and pushes it
 * immediately since they're online by definition - see VotesBridgeListener.
 * Offline players get nothing from a vote party; only whoever's online the
 * moment the target is hit is rewarded.
 */
public interface VotePartyDispatcher {

    /** Called after every credited vote (whether or not it triggers), so %lushvotes_party_*% stays fresh everywhere. */
    void pushProgress(int current, int target);

    void triggerPartyReward();
}
