package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Dispatches already-rendered reward commands (%player% was substituted on
 * the proxy - see RewardCommandRenderer there) as console.
 * {@code Bukkit.dispatchCommand} for the console sender must run on
 * Folia's global tick thread specifically - the entity/region scheduler
 * an earlier version of this class used throws "Dispatching command async"
 * (CraftServer#dispatchCommand asserts the global thread outright,
 * regardless of what the command itself goes on to touch). Same convention
 * as ActionRunner's CONSOLE action.
 */
final class RewardExecutor {

    private final JavaPlugin plugin;

    RewardExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void execute(List<String> commands) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            for (String command : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });
    }
}
