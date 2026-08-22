package com.playgamesinteractive.lushvotes.config;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;

/**
 * Loads {@code config.yml} and {@code language/en_US.yml} from the plugin
 * data directory, copying bundled defaults on first run and backfilling any
 * top-level key a jar update added. See {@link YamlFiles} for the shared
 * file mechanics.
 */
public final class ConfigManager {

    private final Path dataDirectory;
    private final Logger logger;

    private volatile LushVotesConfig config = LushVotesConfig.defaults();
    private volatile Messages messages = Messages.defaults();

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public synchronized void load() {
        loadConfig();
        loadLanguage();
    }

    private void loadConfig() {
        Path file = dataDirectory.resolve("config.yml");
        YamlFiles.copyDefaultIfMissing(file, "config.yml", logger);
        Map<String, Object> root = YamlFiles.read(file, logger);
        root = YamlFiles.mergeMissingTopLevelKeys(file, "config.yml", root, logger);
        this.config = LushVotesConfig.fromMap(root);
    }

    private void loadLanguage() {
        String code = config.language();
        String resourcePath = "language/" + code + ".yml";
        Path file = dataDirectory.resolve(resourcePath);
        YamlFiles.copyDefaultIfMissing(file, "language/en_US.yml", logger);
        Map<String, Object> root = YamlFiles.read(file, logger);
        root = YamlFiles.mergeMissingTopLevelKeys(file, "language/en_US.yml", root, logger);
        this.messages = Messages.of(root);
    }

    public LushVotesConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }
}
