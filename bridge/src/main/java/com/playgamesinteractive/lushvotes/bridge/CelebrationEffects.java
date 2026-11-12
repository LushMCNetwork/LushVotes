package com.playgamesinteractive.lushvotes.bridge;

import com.playgamesinteractive.lushvotes.bridge.lang.LangManager;
import com.playgamesinteractive.lushvotes.bridge.util.FireworkColors;
import com.playgamesinteractive.lushvotes.bridge.util.Sounds;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

final class CelebrationEffects {

    private static final FireworkEffect.Type DEFAULT_FIREWORK_TYPE = FireworkEffect.Type.BALL_LARGE;
    private static final Color DEFAULT_FIREWORK_COLOR = Color.fromRGB(0xFDF700);
    private static final Sound DEFAULT_SOUND = Sound.ENTITY_PLAYER_LEVELUP;

    private final JavaPlugin plugin;
    private final LangManager lang;

    CelebrationEffects(JavaPlugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    void celebrateOnline(Player player, List<String> effects, String fireworkType, String fireworkColor, String soundName) {
        player.getScheduler().run(plugin, task -> {
            for (Component line : lang.lines("reward.online-credited")) {
                player.sendMessage(line);
            }
            if (effects.contains("sound")) {
                player.playSound(player.getLocation(), Sounds.parse(soundName, DEFAULT_SOUND, plugin.getLogger()), 1f, 1f);
            }
            if (effects.contains("firework")) {
                spawnFirework(player, fireworkType, fireworkColor);
            }
        }, null);
    }

    void showClaimResult(Player player, int claimedCount) {
        player.getScheduler().run(plugin, task -> {
            if (claimedCount == 0) {
                player.sendMessage(lang.get("claim.nothing"));
            } else {
                for (Component line : lang.lines("claim.claimed", "count", claimedCount)) {
                    player.sendMessage(line);
                }
            }
        }, null);
    }

    void showUnclaimedReminder(Player player) {
        player.getScheduler().run(plugin, task -> player.sendMessage(lang.get("reminder.unclaimed")), null);
    }

    private void spawnFirework(Player player, String typeName, String colorName) {
        Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(FireworkColors.parse(colorName, DEFAULT_FIREWORK_COLOR, plugin.getLogger()))
                .with(parseFireworkType(typeName))
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private FireworkEffect.Type parseFireworkType(String name) {
        try {
            return FireworkEffect.Type.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown firework type '" + name + "' - using the default instead.");
            return DEFAULT_FIREWORK_TYPE;
        }
    }
}
