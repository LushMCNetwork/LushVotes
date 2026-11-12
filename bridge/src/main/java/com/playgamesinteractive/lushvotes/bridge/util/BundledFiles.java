package com.playgamesinteractive.lushvotes.bridge.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Bundled YAML files (menus, shops, catalogs, language files, ...) are installed <b>once</b>, then
 * belong to the server owner. LushMC network rule (2026-10-23): something an owner deliberately
 * removed - a menu button, a shop entry, a whole file - must stay removed.
 *
 * <ul>
 *   <li>A bundled file is extracted only if this plugin has never installed it before, tracked in
 *       {@code .bundled-installed} in the data folder. Deleting a file keeps it deleted; a file new
 *       in a later plugin version is still installed once. On a server that ran this plugin before
 *       the ledger existed, every bundled file counts as already installed, so nothing currently
 *       deleted comes back.</li>
 *   <li>Existing files are never merged with or overwritten by bundled content.</li>
 *   <li>{@link #settings} files fall back to the bundled values <i>in memory</i> for keys they
 *       don't have, so code always gets a value; nothing is written back.</li>
 *   <li>{@code config.yml}, and any file a plugin passes to {@link #installRequired}, is the
 *       exception to "deleted stays deleted": the plugin can't run without it, so a missing one is
 *       recreated from the bundled default. It is still never merged into.</li>
 * </ul>
 */
public final class BundledFiles {

    private static final String LEDGER = ".bundled-installed";
    private static final Map<String, Ledger> LEDGERS = new ConcurrentHashMap<>();

    private BundledFiles() {}

    /** Menus, shops, catalogs: installed once, then read exactly as the owner left them (empty if deleted). */
    public static YamlConfiguration content(Plugin plugin, String resourcePath) {
        installOnce(plugin, resourcePath);
        File file = new File(plugin.getDataFolder(), resourcePath);
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
    }

    /** Settings or language files: installed once; keys missing on disk read the bundled value, in memory only. */
    public static YamlConfiguration settings(Plugin plugin, String resourcePath) {
        YamlConfiguration onDisk = content(plugin, resourcePath);
        YamlConfiguration bundled = bundled(plugin, resourcePath);
        if (bundled != null) onDisk.setDefaults(bundled);
        return onDisk;
    }

    /** The jar's own copy of a resource, or null if it doesn't bundle one. */
    public static YamlConfiguration bundled(Plugin plugin, String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) return null;
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read bundled " + resourcePath, e);
            return null;
        }
    }

    /**
     * Extracts {@code resourcePath} if (and only if) this plugin has never installed it, or it's
     * {@code config.yml} and missing.
     * @return true if the file now exists on disk
     */
    public static boolean installOnce(Plugin plugin, String resourcePath) {
        return install(plugin, resourcePath, resourcePath.equals("config.yml"));
    }

    /**
     * For files the plugin cannot start without: recreated from the bundled default whenever
     * missing (but, like every other file, never merged into).
     * @return true if the file now exists on disk
     */
    public static boolean installRequired(Plugin plugin, String resourcePath) {
        return install(plugin, resourcePath, true);
    }

    private static boolean install(Plugin plugin, String resourcePath, boolean required) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        Ledger ledger = LEDGERS.computeIfAbsent(plugin.getName(), name -> Ledger.open(plugin));
        if (file.exists()) {
            ledger.record(resourcePath);
            return true;
        }
        if (!required && !ledger.shouldInstall(resourcePath)) {
            ledger.record(resourcePath); // installed (and since deleted) before - remember that permanently
            return false;
        }
        if (plugin.getResource(resourcePath) == null) return false;
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        plugin.saveResource(resourcePath, false);
        ledger.record(resourcePath);
        return file.exists();
    }

    private static final class Ledger {
        private final Plugin plugin;
        private final File file;
        private final Set<String> installed = new LinkedHashSet<>();
        /** True when the plugin ran here before the ledger existed: everything bundled counts as installed. */
        private final boolean adoptExisting;

        private Ledger(Plugin plugin, File file, boolean adoptExisting) {
            this.plugin = plugin;
            this.file = file;
            this.adoptExisting = adoptExisting;
        }

        static Ledger open(Plugin plugin) {
            File folder = plugin.getDataFolder();
            File file = new File(folder, LEDGER);
            boolean existingInstall = !file.exists() && folder.isDirectory() && hasFiles(folder);
            Ledger ledger = new Ledger(plugin, file, existingInstall);
            if (file.exists()) {
                try {
                    for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                        if (!line.isBlank() && !line.startsWith("#")) ledger.installed.add(line.trim());
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not read " + LEDGER, e);
                }
            }
            return ledger;
        }

        private static boolean hasFiles(File folder) {
            File[] children = folder.listFiles();
            return children != null && children.length > 0;
        }

        synchronized boolean shouldInstall(String path) {
            return !installed.contains(path) && !adoptExisting;
        }

        synchronized void record(String path) {
            if (!installed.add(path) && file.exists()) return;
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                StringBuilder text = new StringBuilder(
                        "# Bundled files this plugin has installed. A file listed here that you delete stays deleted.\n");
                for (String entry : installed) text.append(entry).append('\n');
                Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not update " + LEDGER, e);
            }
        }
    }
}
