package com.playgamesinteractive.lushvotes.bridge.command;

import com.playgamesinteractive.lushvotes.bridge.VotesChannel;
import com.playgamesinteractive.lushvotes.bridge.VotesProtocol;
import com.playgamesinteractive.lushvotes.bridge.menu.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * {@code /vote} - opens the menus/vote_menu.yml GUI.
 * {@code /vote claim} - asks the proxy for every queued reward and runs
 * them immediately, instead of them landing automatically on next login
 * (see the design's offline-claim flow) - see VotesChannelListener's
 * PENDING_RESPONSE handling for what happens with the answer.
 */
public final class VoteCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("claim");

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

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        return List.of();
    }

    /** Same case-insensitive prefix filter LushRelay's proxy commands use for tab-complete. */
    private static List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }
}
