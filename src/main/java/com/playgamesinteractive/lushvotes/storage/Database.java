package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single embedded SQLite connection ({@code lushvotes.db} at the
 * plugin data directory root). There's only ever one LushVotes proxy
 * process, so a single-writer embedded DB is enough - no need for
 * LushRelay's shared MySQL. Same convention as LushAuctions/LushShop's
 * Database, ported off {@code JavaPlugin.getDataFolder()} to a raw
 * {@code Path} since this is a Velocity plugin.
 */
public final class Database {

    private final Path dataDirectory;
    private final Logger logger;
    private Connection connection;

    public Database(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
    }

    public void open() {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            logger.error("Failed to create data directory '{}'", dataDirectory, e);
            return;
        }
        Path file = dataDirectory.resolve("lushvotes.db");
        try {
            // Velocity isolates each plugin's classes in their own classloader, so
            // sqlite-jdbc's ServiceLoader-based auto-registration (META-INF/services)
            // often never reaches DriverManager's view from this call site - it ends up
            // registered under a classloader DriverManager's caller-sensitive check
            // won't allow, and getConnection fails with "No suitable driver found" even
            // though the driver class is right there on this plugin's own classpath.
            // Loading it explicitly registers it under this classloader instead.
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                // WAL trades a little extra disk usage for readers (e.g. /lushvotes admin
                // credit checks) never blocking on a concurrent write (a vote being credited).
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS uuid_cache (
                            username TEXT PRIMARY KEY,
                            uuid TEXT NOT NULL,
                            resolved_at INTEGER NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS votes (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            uuid TEXT NOT NULL,
                            username TEXT NOT NULL,
                            service TEXT NOT NULL,
                            credited_at INTEGER NOT NULL
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_votes_uuid_service ON votes(uuid, service, credited_at)");
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS pending_rewards (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            uuid TEXT NOT NULL,
                            username TEXT NOT NULL,
                            service TEXT NOT NULL,
                            created_at INTEGER NOT NULL,
                            delivered_at INTEGER
                        )
                        """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_pending_uuid ON pending_rewards(uuid, delivered_at)");
                addColumnIfMissing(statement, "pending_rewards", "kind", "TEXT NOT NULL DEFAULT 'VOTE'");

                // Singleton row (id is always 1) tracking progress toward the next vote
                // party - see VotePartyRepository. A real row is seeded below rather than
                // relying on callers to insert-or-update, so increment can be one atomic
                // UPDATE ... RETURNING statement.
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS vote_party (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            counter INTEGER NOT NULL DEFAULT 0
                        )
                        """);
                statement.execute("INSERT OR IGNORE INTO vote_party (id, counter) VALUES (1, 0)");
            }
        } catch (ClassNotFoundException e) {
            logger.error("sqlite-jdbc driver class not found - is it missing from the shaded jar?", e);
        } catch (SQLException e) {
            logger.error("Failed to open lushvotes.db", e);
        }
    }

    /** Idempotent - safe to call every startup. SQLite has no "ADD COLUMN IF NOT EXISTS", so check PRAGMA table_info first. */
    private void addColumnIfMissing(Statement statement, String table, String column, String definition) throws SQLException {
        try (ResultSet rs = statement.getConnection().createStatement().executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }
        statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        logger.info("Migrated {}: added '{}' column", table, column);
    }

    public void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.warn("Failed to close lushvotes.db cleanly", e);
        }
    }

    Connection connection() {
        return connection;
    }
}
