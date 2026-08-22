package com.playgamesinteractive.lushvotes.bridge.lang;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@code language/en_US.yml} - copies the jar-bundled default to the
 * plugin data folder on first run, falls back to bundled defaults for any
 * key missing on disk. Same pattern as LushShop/LushAuctions' LangManager;
 * this is a separate copy from LushVotes' own (proxy-side) language file -
 * see that file's header comment for why.
 */
public class LangManager {

    private final JavaPlugin plugin;
    private YamlConfiguration messages;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String resourcePath = "language/en_US.yml";
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(resourcePath, false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                messages.setDefaults(defaults);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load bundled defaults for " + resourcePath + ": " + e.getMessage());
        }
    }

    public String raw(String key, Object... placeholders) {
        String value = messages.getString(key);
        if (value == null) {
            return key;
        }
        return applyPlaceholders(value, placeholders);
    }

    public Component get(String key, Object... placeholders) {
        return ColorText.ofWithLinks(raw(key, placeholders));
    }

    /**
     * A message key that may be authored as either a single string or a
     * YAML list - reward/offline/claim messages need room for a multi-line
     * "here's everything you got" block, but a key with only one line
     * shouldn't be forced into list syntax. Falls back to the key itself
     * (as a single line) if nothing is configured. Each line supports
     * {@code [url](text)} masked links, same as MESSAGE actions.
     */
    public List<Component> lines(String key, Object... placeholders) {
        if (messages.isList(key)) {
            List<Component> result = new ArrayList<>();
            for (String line : messages.getStringList(key)) {
                result.add(ColorText.ofWithLinks(applyPlaceholders(line, placeholders)));
            }
            return result;
        }
        String single = messages.getString(key);
        return List.of(ColorText.ofWithLinks(single == null ? key : applyPlaceholders(single, placeholders)));
    }

    private String applyPlaceholders(String value, Object... placeholders) {
        String result = value;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
        }
        return result;
    }
}
