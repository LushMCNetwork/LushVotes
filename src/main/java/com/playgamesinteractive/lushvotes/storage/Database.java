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
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            try (Statement statement = connection.createStatement()) {
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

                statement.execute("""
                        CREATE TABLE IF NOT EXISTS vote_party (
                            id INTEGER PRIMARY KEY CHECK (id = 1),
                            counter INTEGER NOT NULL DEFAULT 0
                        )
                        """);
                statement.execute("INSERT OR IGNORE INTO vote_party (id, counter) VALUES (1, 0)");
                forgetPrefixedNames(statement);
            }
        } catch (ClassNotFoundException e) {
            logger.error("sqlite-jdbc driver class not found - is it missing from the shaded jar?", e);
        } catch (SQLException e) {
            logger.error("Failed to open lushvotes.db", e);
        }
    }

    /**
     * Version 1: names starting with Floodgate's prefix used to go straight to Mojang, which can
     * return an unrelated Java account. Those cached answers are dropped so they resolve again.
     */
    private void forgetPrefixedNames(Statement statement) throws SQLException {
        int version;
        try (ResultSet rs = statement.executeQuery("PRAGMA user_version")) {
            version = rs.next() ? rs.getInt(1) : 0;
        }
        if (version >= 1) return;
        int dropped = statement.executeUpdate("DELETE FROM uuid_cache WHERE username LIKE '\\_%' ESCAPE '\\'");
        statement.execute("PRAGMA user_version = 1");
        if (dropped > 0) logger.info("Dropped {} cached UUID(s) for Bedrock-style names so they resolve again", dropped);
    }

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
