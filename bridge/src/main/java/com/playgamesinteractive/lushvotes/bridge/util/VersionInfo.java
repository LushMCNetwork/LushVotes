package com.playgamesinteractive.lushvotes.bridge.util;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionInfo {

    private static String commit = "unknown";
    private static String built = "unknown";
    private static boolean loaded = false;

    private VersionInfo() {
    }

    public static void load(Plugin plugin) {
        if (loaded) {
            return;
        }
        loaded = true;
        try (InputStream stream = plugin.getResource("version.properties")) {
            if (stream == null) {
                return;
            }
            Properties properties = new Properties();
            properties.load(stream);
            commit = properties.getProperty("commit", commit);
            built = properties.getProperty("built", built);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read version.properties: " + e.getMessage());
        }
    }

    public static String commit() {
        return commit;
    }

    public static String built() {
        return built;
    }
}
