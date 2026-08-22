package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * On every join: asks the proxy how many rewards are waiting (informational
 * only - see VotesChannelListener's PENDING_COUNT_RESPONSE handling, which
 * just nudges the player toward {@code /vote claim} rather than executing
 * anything), and separately requests a config-sync catch-up if none has
 * arrived yet - covers a plugin-only reload with players already online,
 * where no new connection ever fires on the proxy side to hang a push off
 * of (same gap ConfigSyncRequester documents in LushRelay). Also clears the
 * quitting player's {@link VoteStatsCache} entry.
 */
final class RewardJoinRequester implements Listener {

    private final JavaPlugin plugin;
    private final CelebrationConfigCache configCache;
    private final VoteStatsCache statsCache;

    RewardJoinRequester(JavaPlugin plugin, CelebrationConfigCache configCache, VoteStatsCache statsCache) {
        this.plugin = plugin;
        this.configCache = configCache;
        this.statsCache = statsCache;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestPendingCount(player.getUniqueId()));
        if (!configCache.isLoaded()) {
            player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestSync());
        }
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
