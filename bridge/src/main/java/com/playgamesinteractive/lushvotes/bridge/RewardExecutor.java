package com.playgamesinteractive.lushvotes.bridge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Runs vote reward commands as console. The proxy sends the same reward list to every server, so a
 * command this server doesn't have (such as Raft's {@code raft admin give ...} on Earth
 * Frontier) is skipped instead of failing.
 */
final class RewardExecutor {

    private final JavaPlugin plugin;

    RewardExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void execute(List<String> commands) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            for (String command : commands) {
                String root = command.strip().split("\\s+", 2)[0];
                if (Bukkit.getCommandMap().getCommand(root) == null) {
                    plugin.getLogger().fine("Skipping vote reward command not available here: " + root);
                    continue;
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        });
    }
}
