package com.playgamesinteractive.lushvotes.config;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;

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
        YamlFiles.install(dataDirectory, "config.yml", true, logger);
        Map<String, Object> root = YamlFiles.withBundledDefaults("config.yml", YamlFiles.read(file, logger), logger);
        this.config = LushVotesConfig.fromMap(root);
    }

    private void loadLanguage() {
        String code = config.language();
        String resourcePath = "language/" + code + ".yml";
        Path file = dataDirectory.resolve(resourcePath);
        YamlFiles.install(dataDirectory, resourcePath, false, logger);
        Map<String, Object> root = YamlFiles.withBundledDefaults("language/en_US.yml", YamlFiles.read(file, logger), logger);
        this.messages = Messages.of(root);
    }

    public LushVotesConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }
}
