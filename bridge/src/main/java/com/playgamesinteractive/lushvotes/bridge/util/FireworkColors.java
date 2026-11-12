package com.playgamesinteractive.lushvotes.bridge.util;

import org.bukkit.Color;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.logging.Logger;

public final class FireworkColors {

    private FireworkColors() {
    }

    public static Color parse(String value, Color def, Logger logger) {
        String trimmed = value.trim();
        if (trimmed.startsWith("#")) {
            return parseHex(trimmed, def, logger);
        }
        try {
            Field field = Color.class.getField(trimmed.toUpperCase(Locale.ROOT));
            Object result = field.get(null);
            if (result instanceof Color color) {
                return color;
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        logger.warning("Unknown firework color '" + value + "' - using the default instead.");
        return def;
    }

    private static Color parseHex(String hex, Color def, Logger logger) {
        String digits = hex.substring(1);
        if (digits.length() != 6) {
            logger.warning("Invalid hex firework color '" + hex + "' - expected '#rrggbb'. Using the default instead.");
            return def;
        }
        try {
            return Color.fromRGB(Integer.parseInt(digits, 16));
        } catch (NumberFormatException e) {
            logger.warning("Invalid hex firework color '" + hex + "' - using the default instead.");
            return def;
        }
    }
}
