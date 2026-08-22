package com.playgamesinteractive.lushvotes.vote;

import com.playgamesinteractive.lushvotes.storage.PendingReward;

/**
 * Attempts immediate delivery of a freshly-credited reward. Implemented by
 * the bridge-channel layer, which pushes the reward to the player's current
 * backend if they're online right now, or does nothing if they're offline -
 * offline delivery instead happens when a backend asks for pending rewards
 * on that player's next join (see the design's step 5/6).
 */
public interface RewardDispatcher {

    void deliverNow(PendingReward reward);
}
