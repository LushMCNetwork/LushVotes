package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Decodes every message LushVotes (proxy) sends over {@link VotesChannel}.
 * See VotesBridgeListener on the proxy side for the encoder.
 * <ul>
 *   <li>{@code CONFIG_SYNC} - mirrors celebration effects locally.</li>
 *   <li>{@code PARTY_PROGRESS} - mirrors vote party progress locally, for
 *   %lushvotes_party_*%.</li>
 *   <li>{@code REWARD_NOW} - the player is online right now; run the reward
 *   and celebrate immediately.</li>
 *   <li>{@code PENDING_RESPONSE} - answers this backend's own
 *   {@code REQUEST_CLAIM} (sent by {@code /vote claim}); executes every
 *   queued reward and shows the claim result.</li>
 *   <li>{@code PENDING_COUNT_RESPONSE} - answers this backend's own
 *   {@code REQUEST_PENDING_COUNT} (sent on join); purely informational, a
 *   nudge to run /vote claim, nothing is executed here.</li>
 * </ul>
 */
final class VotesChannelListener implements PluginMessageListener {

    private final JavaPlugin plugin;
    private final CelebrationConfigCache configCache;
    private final VoteStatsCache statsCache;
    private final VotePartyCache partyCache;
    private final RewardExecutor rewardExecutor;
    private final CelebrationEffects celebrationEffects;
    private final Logger logger;

    VotesChannelListener(JavaPlugin plugin, CelebrationConfigCache configCache, VoteStatsCache statsCache,
                          VotePartyCache partyCache, RewardExecutor rewardExecutor, CelebrationEffects celebrationEffects,
                          Logger logger) {
        this.plugin = plugin;
        this.configCache = configCache;
        this.statsCache = statsCache;
        this.partyCache = partyCache;
        this.rewardExecutor = rewardExecutor;
        this.celebrationEffects = celebrationEffects;
        this.logger = logger;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player carrier, byte[] message) {
        if (!VotesChannel.NAME.equals(channel)) {
            return;
        }
        try {
            switch (VotesProtocol.opcodeOf(message)) {
                case VotesProtocol.OPCODE_CONFIG_SYNC -> handleConfigSync(message);
                case VotesProtocol.OPCODE_PARTY_PROGRESS -> handlePartyProgress(message);
                case VotesProtocol.OPCODE_REWARD_NOW -> handleRewardNow(message);
                case VotesProtocol.OPCODE_PENDING_RESPONSE -> handlePendingResponse(message);
                case VotesProtocol.OPCODE_PENDING_COUNT_RESPONSE -> handlePendingCountResponse(message);
                default -> logger.warn("Unknown LushVotes opcode {} received", VotesProtocol.opcodeOf(message));
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.warn("Rejected malformed LushVotes proxy message", e);
        }
    }

    private void handleConfigSync(byte[] message) throws IOException {
        VotesProtocol.ConfigSync sync = VotesProtocol.decodeConfigSync(message);
        configCache.update(sync.effects(), sync.fireworkType(), sync.fireworkColor(), sync.sound());
    }

    private void handlePartyProgress(byte[] message) throws IOException {
        VotesProtocol.PartyProgress progress = VotesProtocol.decodePartyProgress(message);
        partyCache.update(progress.current(), progress.target());
    }

    private void handleRewardNow(byte[] message) throws IOException {
        VotesProtocol.RewardNow reward = VotesProtocol.decodeRewardNow(message);
        statsCache.update(reward.uuid(), reward.stats().totalVotes(), reward.stats().lastVoteAtEpochMillis());
        Player player = Bukkit.getPlayer(reward.uuid());
        if (player == null) {
            // Disconnected between the proxy crediting this and the message arriving -
            // their next /vote claim will pick it up instead.
            return;
        }
        rewardExecutor.execute(reward.commands());
        celebrationEffects.celebrateOnline(player, configCache.effects(), configCache.fireworkType(), configCache.fireworkColor(), configCache.sound());
        send(player, VotesProtocol.encodeDeliveredAck(reward.pendingRewardId()));
    }

    /** Answers /vote claim - executes everything queued and shows the result, regardless of count. */
    private void handlePendingResponse(byte[] message) throws IOException {
        VotesProtocol.PendingResponse response = VotesProtocol.decodePendingResponse(message);
        statsCache.update(response.uuid(), response.stats().totalVotes(), response.stats().lastVoteAtEpochMillis());
        Player player = Bukkit.getPlayer(response.uuid());
        if (player == null) {
            return; // left before the claim response arrived
        }
        for (VotesProtocol.RewardCommands reward : response.rewards()) {
            rewardExecutor.execute(reward.commands());
            send(player, VotesProtocol.encodeDeliveredAck(reward.pendingRewardId()));
        }
        celebrationEffects.showClaimResult(player, response.rewards().size());
    }

    /** Answers the join-time reminder check - never executes or acks anything. */
    private void handlePendingCountResponse(byte[] message) throws IOException {
        VotesProtocol.PendingCount count = VotesProtocol.decodePendingCountResponse(message);
        if (count.count() <= 0) {
            return;
        }
        Player player = Bukkit.getPlayer(count.uuid());
        if (player != null) {
            celebrationEffects.showUnclaimedReminder(player);
        }
    }

    private void send(Player player, byte[] payload) {
        player.sendPluginMessage(plugin, VotesChannel.NAME, payload);
    }
}
