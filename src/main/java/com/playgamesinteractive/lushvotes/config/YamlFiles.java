package com.playgamesinteractive.lushvotes.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raw-SnakeYAML equivalent of LushShop/LushAuctions' YamlDefaultsMerger, for
 * Velocity plugins - there's no {@code JavaPlugin.saveDefaultConfig()} or
 * {@code ConfigurationSection} on this platform, so config here is a plain
 * {@code Map<String, Object>} read/written by hand. Same rationale and same
 * "only fill in a key that's entirely absent" behavior as the Bukkit-side
 * version, ported per LushRelay's ConfigManager convention.
 */
final class YamlFiles {

    private YamlFiles() {
    }

    /** Copies the jar-bundled resource to {@code target} if nothing's there yet. */
    static void copyDefaultIfMissing(Path target, String resourcePath, Logger logger) {
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = YamlFiles.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    logger.error("Bundled resource '{}' is missing from the jar - cannot create it.", resourcePath);
                    return;
                }
                Files.copy(in, target);
            }
        } catch (IOException e) {
            logger.error("Failed to create '{}' from the bundled default", target, e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> read(Path file, Logger logger) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Object loaded = new Yaml().load(text);
            if (loaded instanceof Map<?, ?> m) {
                return (Map<String, Object>) m;
            }
            return new LinkedHashMap<>();
        } catch (IOException e) {
            logger.error("Failed to read '{}'", file, e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * Backfills top-level keys present in the bundled resource but absent
     * from the live file, then rewrites the live file if anything changed.
     * Deliberately shallow (top-level keys only) - a list like {@code sites}
     * or {@code reward.commands} is admin-curated content; once the key
     * exists at all, this never reaches inside it.
     */
    static Map<String, Object> mergeMissingTopLevelKeys(Path file, String resourcePath, Map<String, Object> live, Logger logger) {
        Map<String, Object> defaults = readBundled(resourcePath, logger);
        boolean changed = false;
        Map<String, Object> merged = new LinkedHashMap<>(live);
        for (Map.Entry<String, Object> entry : defaults.entrySet()) {
            if (!merged.containsKey(entry.getKey())) {
                merged.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            write(file, merged, logger);
            logger.info("Added new default option(s) to '{}' introduced by this update - check the file to tune them.", file);
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readBundled(String resourcePath, Logger logger) {
        try (InputStream in = YamlFiles.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return new LinkedHashMap<>();
            }
            Object loaded = new Yaml().load(in);
            return loaded instanceof Map<?, ?> m ? (Map<String, Object>) m : new LinkedHashMap<>();
        } catch (IOException e) {
            logger.error("Failed to read bundled resource '{}'", resourcePath, e);
            return new LinkedHashMap<>();
        }
    }

    private static void write(Path file, Map<String, Object> data, Logger logger) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            logger.error("Failed to write '{}'", file, e);
        }
    }
}
