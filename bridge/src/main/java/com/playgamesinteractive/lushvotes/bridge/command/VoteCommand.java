package com.playgamesinteractive.lushvotes.bridge.command;

import com.playgamesinteractive.lushvotes.bridge.VotesChannel;
import com.playgamesinteractive.lushvotes.bridge.VotesProtocol;
import com.playgamesinteractive.lushvotes.bridge.menu.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /vote} - opens the menus/vote_menu.yml GUI.
 * {@code /vote claim} - asks the proxy for every queued reward and runs
 * them immediately, instead of them landing automatically on next login
 * (see the design's offline-claim flow) - see VotesChannelListener's
 * PENDING_RESPONSE handling for what happens with the answer.
 */
public final class VoteCommand implements CommandExecutor {

    private final MenuManager menuManager;
    private final Plugin plugin;

    public VoteCommand(MenuManager menuManager, Plugin plugin) {
        this.menuManager = menuManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can vote.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("claim")) {
            player.sendPluginMessage(plugin, VotesChannel.NAME, VotesProtocol.encodeRequestClaim(player.getUniqueId()));
            return true;
        }
        menuManager.open(player, "vote_menu");
        return true;
    }
}
