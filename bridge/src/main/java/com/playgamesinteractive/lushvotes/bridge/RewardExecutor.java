package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
