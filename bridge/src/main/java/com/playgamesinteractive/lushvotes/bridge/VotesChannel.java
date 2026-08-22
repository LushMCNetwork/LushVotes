package com.playgamesinteractive.lushvotes.bridge;

/**
 * Bukkit-side plugin-messaging channel name mirroring VotesBridgeChannel on
 * the proxy. Public (not package-private like most of this package) since
 * {@code command.VoteCommand} needs it to send the /vote claim request.
 */
public final class VotesChannel {

    public static final String NAME = "lushvotes:sync";

    private VotesChannel() {
    }
}
