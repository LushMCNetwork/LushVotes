package com.playgamesinteractive.lushvotes.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bundled files (language, menus, settings...) are installed <b>once</b>, then belong to the
 * operator. LushMC network rule (2026-10-23): something an operator deliberately removed - a key,
 * a menu entry, a whole file - must stay removed. Same rules as the Bukkit plugins' copy:
 *
 * <ul>
 *   <li>A bundled file is extracted only if this service has never installed it before, tracked in
 *       {@code .bundled-installed} in the data directory. Deleting a file keeps it deleted; a file
 *       new in a later version is still installed once. On an install that predates the ledger,
 *       every bundled file counts as already installed.</li>
 *   <li>Existing files are never merged with or overwritten by bundled content; callers fall back
 *       to bundled values in memory instead.</li>
 *   <li>{@link #installRequired} files (e.g. {@code config.yml}) are recreated whenever missing.</li>
 * </ul>
 */
public final class BundledFiles {

    private static final String LEDGER = ".bundled-installed";
    private static final Map<Path, Ledger> LEDGERS = new ConcurrentHashMap<>();

    private BundledFiles() {}

    /** @return true if the file now exists */
    public static boolean installOnce(Path dataDir, String resourcePath, ClassLoader loader) throws IOException {
        return install(dataDir, resourcePath, loader, false);
    }

    /** For files the service cannot run without: recreated whenever missing, never merged into. */
    public static boolean installRequired(Path dataDir, String resourcePath, ClassLoader loader) throws IOException {
        return install(dataDir, resourcePath, loader, true);
    }

    private static boolean install(Path dataDir, String resourcePath, ClassLoader loader, boolean required)
            throws IOException {
        Path file = dataDir.resolve(resourcePath);
        Ledger ledger = LEDGERS.computeIfAbsent(dataDir.toAbsolutePath().normalize(), Ledger::open);
        if (Files.exists(file)) {
            ledger.record(resourcePath);
            return true;
        }
        if (!required && !ledger.shouldInstall(resourcePath)) {
            ledger.record(resourcePath); // installed (and since deleted) before - remember that permanently
            return false;
        }
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) return false;
            Files.createDirectories(file.getParent());
            Files.copy(in, file);
        }
        ledger.record(resourcePath);
        return true;
    }

    private static final class Ledger {
        private final Path file;
        private final Set<String> installed = new LinkedHashSet<>();
        private final boolean adoptExisting;

        private Ledger(Path file, boolean adoptExisting) {
            this.file = file;
            this.adoptExisting = adoptExisting;
        }

        static Ledger open(Path dataDir) {
            Path file = dataDir.resolve(LEDGER);
            boolean existingInstall = false;
            try {
                if (!Files.exists(file) && Files.isDirectory(dataDir)) {
                    try (var children = Files.list(dataDir)) {
                        existingInstall = children.findAny().isPresent();
                    }
                }
                Ledger ledger = new Ledger(file, existingInstall);
                if (Files.exists(file)) {
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        if (!line.isBlank() && !line.startsWith("#")) ledger.installed.add(line.trim());
                    }
                }
                return ledger;
            } catch (IOException e) {
                return new Ledger(file, true); // can't read state: never risk re-adding a removed file
            }
        }

        synchronized boolean shouldInstall(String path) {
            return !installed.contains(path) && !adoptExisting;
        }

        synchronized void record(String path) {
            if (!installed.add(path) && Files.exists(file)) return;
            try {
                Files.createDirectories(file.getParent());
                StringBuilder text = new StringBuilder(
                        "# Bundled files this service has installed. A file listed here that you delete stays deleted.\n");
                for (String entry : installed) text.append(entry).append('\n');
                Files.writeString(file, text, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // Best effort; the in-memory record still covers this run.
            }
        }
    }
}
