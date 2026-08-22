package com.playgamesinteractive.lushvotes.bridge.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * The single %placeholder% pipeline for player-facing config text (menu
 * actions/titles): %player%/%player_name% always resolve, then
 * PlaceholderAPI runs when it's installed so any %papi_...% (including
 * %lushvotes_*%) works in the same strings without a hard dependency. Same
 * convention as lushlobby's Placeholders.
 */
public final class Placeholders {

    private Placeholders() {
    }

    public static String apply(Player player, String text) {
        text = text.replace("%player%", player.getName()).replace("%player_name%", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
}
