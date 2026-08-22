package com.playgamesinteractive.lushvotes.vote;

import com.velocitypowered.api.event.Subscribe;
import com.vexsoftware.votifier.velocity.event.VotifierEvent;
import org.slf4j.Logger;

/**
 * Subscribes to NuVotifier-Velocity's own internal event bus. Forwarding is
 * deliberately off on this network (see the design doc's "Why two
 * artifacts" section) - NuVotifier's plugin-messaging forwarding drops a
 * vote outright if no one's online anywhere to relay it, so this is the
 * only place a vote is ever guaranteed to be seen at all.
 */
public final class VoteListener {

    private final VoteService service;
    private final Logger logger;

    public VoteListener(VoteService service, Logger logger) {
        this.service = service;
        this.logger = logger;
    }

    @Subscribe
    public void onVote(VotifierEvent event) {
        String username = normalizeUsername(event.getVote().getUsername());
        String service = event.getVote().getServiceName();
        if (username.isEmpty()) {
            logger.warn("Ignored a vote with an empty username from service '{}'", service);
            return;
        }
        this.service.processVote(username, service).whenComplete((result, error) -> {
            if (error != null) {
                logger.error("Unexpected failure processing vote for '{}' on '{}'", username, service, error);
            }
        });
    }

    /** Trims stray whitespace some vote sites are known to send. Pure so it's testable on its own. */
    static String normalizeUsername(String raw) {
        return raw == null ? "" : raw.strip();
    }
}
