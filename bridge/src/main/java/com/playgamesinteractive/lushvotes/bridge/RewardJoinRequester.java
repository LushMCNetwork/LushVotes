package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * On every join: asks the proxy how many rewards are waiting (informational
 * only - see VotesChannelListener's PENDING_COUNT_RESPONSE handling, which
 * just nudges the player toward {@code /vote claim} rather than executing
 * anything), requests this player's real vote totals (see
 * VotesProtocol.OPCODE_REQUEST_STATS - otherwise %lushvotes_total% reads 0
 * until their next vote or claim, no matter how many they've actually
 * cast), and separately requests a config-sync catch-up if none has
 * arrived yet - covers a plugin-only reload with players already online,
 * where no new connection ever fires on the proxy side to hang a push off
 * of (same gap ConfigSyncRequester documents in LushRelay). Also clears the
 * quitting player's {@link VoteStatsCache} entry.
 */
final class RewardJoinRequester implements Listener {

    private final JavaPlugin plugin;
    private final CelebrationConfigCache configCache;
    private final VoteStatsCache statsCache;
    private final Logger logger;

    RewardJoinRequester(JavaPlugin plugin, CelebrationConfigCache configCache, VoteStatsCache statsCache, Logger logger) {
        this.plugin = plugin;
        this.configCache = configCache;
        this.statsCache = statsCache;
        this.logger = logger;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Sent one tick late on purpose: a plugin message fired synchronously inside
        // PlayerJoinEvent races the backend<->proxy connection settling and was being
        // silently dropped before it ever reached Velocity. Delayed via the player's own
        // per-entity region scheduler rather than Bukkit.getScheduler() - this server runs
        // Folia (see pom.xml's folia-api dependency), where the legacy global scheduler
        // throws UnsupportedOperationException.
        player.getScheduler().runDelayed(plugin, task -> {
            player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestPendingCount(player.getUniqueId()));
            player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestStats(player.getUniqueId()));
            if (!configCache.isLoaded()) {
                player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestSync());
            }
        }, /* retired: player left before the tick fired */ null, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        statsCache.clear(event.getPlayer().getUniqueId());
    }

    /** Called once at enable-time to cover an already-populated server (see class doc). */
    void requestConfigSyncFromAnyOnlinePlayer() {
        if (configCache.isLoaded()) {
            return;
        }
        Bukkit.getOnlinePlayers().stream().findFirst().ifPresent(player ->
                player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestSync()));
    }
}
