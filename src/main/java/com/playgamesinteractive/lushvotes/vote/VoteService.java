package com.playgamesinteractive.lushvotes.vote;

import com.playgamesinteractive.lushvotes.config.ConfigManager;
import com.playgamesinteractive.lushvotes.mojang.MojangResolver;
import com.playgamesinteractive.lushvotes.storage.PendingReward;
import com.playgamesinteractive.lushvotes.storage.PendingRewardRepository;
import com.playgamesinteractive.lushvotes.storage.RewardKind;
import com.playgamesinteractive.lushvotes.storage.VotePartyRepository;
import com.playgamesinteractive.lushvotes.storage.VoteRepository;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class VoteService {

    private final MojangResolver resolver;
    private final VoteRepository votes;
    private final PendingRewardRepository pending;
    private final VotePartyRepository votePartyRepository;
    private final ConfigManager configManager;
    private final Clock clock;
    private final RewardDispatcher dispatcher;
    private final VotePartyDispatcher partyDispatcher;
    private final Logger logger;

    public VoteService(MojangResolver resolver, VoteRepository votes, PendingRewardRepository pending,
                        VotePartyRepository votePartyRepository, ConfigManager configManager, Clock clock,
                        RewardDispatcher dispatcher, VotePartyDispatcher partyDispatcher, Logger logger) {
        this.resolver = resolver;
        this.votes = votes;
        this.pending = pending;
        this.votePartyRepository = votePartyRepository;
        this.configManager = configManager;
        this.clock = clock;
        this.dispatcher = dispatcher;
        this.partyDispatcher = partyDispatcher;
        this.logger = logger;
    }

    /** Normal vote path: always counts toward the vote party. */
    public CompletableFuture<CreditResult> processVote(String username, String service) {
        return credit(username, service, true);
    }

    /** {@code /lushvotes admin credit} path: recovers a real vote that failed to credit, always counts toward the party. */
    public CompletableFuture<CreditResult> creditManual(String username, String service) {
        return credit(username, service, true);
    }

    public CompletableFuture<CreditResult> creditTestVote(String username, String service, boolean countTowardParty) {
        return credit(username, service, countTowardParty);
    }

    private CompletableFuture<CreditResult> credit(String username, String service, boolean countTowardParty) {
        return resolver.resolve(username).thenApply(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                return CreditResult.rejected(null, "uuid-resolution-failed");
            }
            UUID uuid = uuidOpt.get();

            Instant now = Instant.now(clock);
            votes.record(uuid, username, service, now);
            long pendingId = pending.create(uuid, username, service, now, RewardKind.VOTE);
            if (pendingId >= 0) {
                dispatcher.deliverNow(new PendingReward(pendingId, uuid, username, service, now, null, RewardKind.VOTE));
            }

            if (countTowardParty) {
                advanceVoteParty();
            }

            return CreditResult.credited(uuid);
        });
    }

    private void advanceVoteParty() {
        int target = configManager.config().votePartyTarget();
        int current = votePartyRepository.incrementAndGet();
        if (current >= target) {
            votePartyRepository.set(0);
            current = 0;
            logger.info("Vote party target of {} reached - rewarding every online player.", target);
            partyDispatcher.triggerPartyReward();
        }
        partyDispatcher.pushProgress(current, target);
    }
}
