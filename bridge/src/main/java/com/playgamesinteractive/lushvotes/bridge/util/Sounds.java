package com.playgamesinteractive.lushvotes.bridge.util;

import org.bukkit.Sound;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.logging.Logger;

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
        }
        logger.warning("Unknown sound '" + name + "' - using the default instead.");
        return def;
    }
}
