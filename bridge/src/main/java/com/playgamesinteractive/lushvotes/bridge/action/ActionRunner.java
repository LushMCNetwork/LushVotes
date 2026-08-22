package com.playgamesinteractive.lushvotes.bridge.action;

import com.playgamesinteractive.lushvotes.bridge.lang.ColorText;
import com.playgamesinteractive.lushvotes.bridge.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Executes a parsed Action list against a player. CONSOLE commands are
 * dispatched via the global region scheduler since they may touch state
 * outside the player's own region - PLAYER and MESSAGE run inline, since
 * callers only ever invoke this from an event already running on that
 * player's own thread (an inventory click). Same convention and rationale
 * as lushlobby's ActionRunner. MenuListener already closes the inventory
 * before running any actions, so there's no separate "close" action needed.
 */
public class ActionRunner {

    private final Plugin plugin;

    public ActionRunner(Plugin plugin) {
        this.plugin = plugin;
    }

    public void run(List<Action> actions, Player player) {
        for (Action action : actions) {
            String argument = Placeholders.apply(player, action.argument());
            switch (action.type()) {
                case MESSAGE -> player.sendMessage(ColorText.ofWithLinks(argument));
                case PLAYER -> Bukkit.dispatchCommand(player, argument);
                case CONSOLE -> Bukkit.getGlobalRegionScheduler().execute(plugin,
                        () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), argument));
            }
        }
    }
}
