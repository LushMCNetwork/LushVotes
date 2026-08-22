package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Dispatches already-rendered reward commands (%player% was substituted on
 * the proxy - see RewardCommandRenderer there) as console.
 * Hops onto the target player's own scheduler first: a reward command may
 * touch their inventory or location (e.g. a "give" alongside an "eco
 * give"), so this can't be pure global-scheduler bookkeeping under Folia.
 */
final class RewardExecutor {

    private final JavaPlugin plugin;

    RewardExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void execute(Player player, List<String> commands) {
        player.getScheduler().run(plugin, task -> {
            for (String command : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }, null);
    }
}
