package com.playgamesinteractive.lushvotes.bridge.util;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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
