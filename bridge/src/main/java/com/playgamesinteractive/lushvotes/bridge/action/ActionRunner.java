package com.playgamesinteractive.lushvotes.bridge.action;

import com.playgamesinteractive.lushvotes.bridge.lang.ColorText;
import com.playgamesinteractive.lushvotes.bridge.util.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

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
