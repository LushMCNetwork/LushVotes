package com.playgamesinteractive.lushvotes.bridge;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

/**
 * The "lushvotes:sync" plugin-messaging channel between LushVotes and
 * LushVotesBridge. Not related to NuVotifier forwarding (which is off) -
 * this is LushVotes' own channel, always available regardless of that
 * setting, same posture as LushRelay's bridge channels.
 */
public final class VotesBridgeChannel {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.create("lushvotes", "sync");

    private VotesBridgeChannel() {
    }
}
