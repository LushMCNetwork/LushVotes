package com.playgamesinteractive.lushvotes.vote;

public interface VotePartyDispatcher {

    void pushProgress(int current, int target);

    void triggerPartyReward();
}
