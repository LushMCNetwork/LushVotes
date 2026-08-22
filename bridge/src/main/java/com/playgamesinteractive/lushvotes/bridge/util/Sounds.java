package com.playgamesinteractive.lushvotes.bridge.util;

import org.bukkit.Sound;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Resolves a config-authored sound name (e.g. "ENTITY_PLAYER_LEVELUP")
 * against {@code org.bukkit.Sound}'s public static fields via reflection,
 * rather than the deprecated-and-marked-for-removal {@code Sound.valueOf}.
 * The alternative - {@code Registry.SOUNDS.get(NamespacedKey)} - needs a
 * dotted key ("entity.player.levelup"), a different format than the
 * underscored constant names this config already uses; reflection keeps
 * that same familiar naming instead of asking admins to learn a second
 * format. Same fallback shape as {@link FireworkColors} - no unit test
 * alongside it, though: merely touching {@code org.bukkit.Sound} triggers
 * its static initializer, which needs a live Paper {@code RegistryAccess}
 * only a running server provides, so it can't load in a plain JUnit run the
 * way {@code Color} (a plain data class, not registry-backed) can.
 */
public final class Sounds {

    private Sounds() {
    }

    public static Sound parse(String name, Sound def, Logger logger) {
        try {
            Field field = Sound.class.getField(name.toUpperCase(Locale.ROOT));
            Object result = field.get(null);
            if (result instanceof Sound sound) {
                return sound;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // falls through to the warning below
        }
        logger.warning("Unknown sound '" + name + "' - using the default instead.");
        return def;
    }
}
