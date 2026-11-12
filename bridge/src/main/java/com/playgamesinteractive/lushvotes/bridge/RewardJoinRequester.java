package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

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

    void requestConfigSyncFromAnyOnlinePlayer() {
        if (configCache.isLoaded()) {
            return;
        }
        Bukkit.getOnlinePlayers().stream().findFirst().ifPresent(player ->
                player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestSync()));
    }
}
