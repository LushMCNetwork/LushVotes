package com.playgamesinteractive.lushvotes.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class YamlFiles {

    private YamlFiles() {
    }

    /** Install-once (see {@link BundledFiles}); {@code required} files are recreated if deleted. */
    static void install(Path dataDirectory, String resourcePath, boolean required, Logger logger) {
        try {
            ClassLoader loader = YamlFiles.class.getClassLoader();
            if (required) BundledFiles.installRequired(dataDirectory, resourcePath, loader);
            else BundledFiles.installOnce(dataDirectory, resourcePath, loader);
        } catch (IOException e) {
            logger.error("Failed to install '{}' from the bundled default", resourcePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> read(Path file, Logger logger) {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
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
     * The operator's file with any top-level key it lacks filled in from the bundled copy - in
     * memory only. The file itself is never rewritten, so nothing they removed comes back.
     */
    static Map<String, Object> withBundledDefaults(String resourcePath, Map<String, Object> live, Logger logger) {
        Map<String, Object> merged = new LinkedHashMap<>(readBundled(resourcePath, logger));
        merged.putAll(live);
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
}
