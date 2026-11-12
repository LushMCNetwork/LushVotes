package com.playgamesinteractive.lushvotes.bridge;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public final class VotesBridgeChannel {

    public static final MinecraftChannelIdentifier IDENTIFIER = MinecraftChannelIdentifier.create("lushvotes", "sync");

    private VotesBridgeChannel() {
    }
}
