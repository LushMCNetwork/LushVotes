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
