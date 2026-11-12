package com.playgamesinteractive.lushvotes.vote;

import com.playgamesinteractive.lushvotes.storage.PendingReward;

public interface RewardDispatcher {

    void deliverNow(PendingReward reward);
}
