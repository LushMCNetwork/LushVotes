package com.playgamesinteractive.lushvotes.storage;

/**
 * Which reward-command template a pending_rewards row renders against at
 * delivery time - {@code reward.commands} for a normal vote, {@code
 * vote-party.commands} when the network hit its vote party target. See
 * VotesBridgeListener, which does the actual template selection.
 */
public enum RewardKind {
    VOTE,
    PARTY
}
