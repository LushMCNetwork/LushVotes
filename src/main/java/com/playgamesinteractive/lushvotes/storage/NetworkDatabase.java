package com.playgamesinteractive.lushvotes.storage;

import com.playgamesinteractive.lushvotes.config.LushVotesConfig;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The network MySQL database the website reads. lushvotes.db stays the source of truth: every
 * vote is copied to {@code lush_votes} under its SQLite id, so a sync after an outage simply
 * copies whatever is newer than the highest id already there. LushStats on the game servers
 * fills {@code lush_players}, which is read here to resolve names the network has seen.
 */
public final class NetworkDatabase {

    private static final int BATCH = 500;

    private final LushVotesConfig.NetworkDatabase settings;
    private final VoteRepository votes;
    private final Logger logger;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "LushVotes-network-db");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean syncQueued = new AtomicBoolean();
    private Connection connection;

    public NetworkDatabase(LushVotesConfig.NetworkDatabase settings, VoteRepository votes, Logger logger) {
        this.settings = settings;
        this.votes = votes;
        this.logger = logger;
    }

    public boolean enabled() {
        return settings.enabled();
    }

    /** Queues a copy of every vote not yet in MySQL. Calls while one is queued are merged. */
    public void requestSync() {
        if (!enabled() || !syncQueued.compareAndSet(false, true)) return;
        worker.execute(() -> {
            syncQueued.set(false);
            try {
                int copied = sync();
                if (copied > 0) logger.info("Copied {} vote(s) to the network database.", copied);
            } catch (SQLException e) {
                logger.warn("Could not copy votes to the network database: {}", e.getMessage());
                closeQuietly();
            }
        });
    }

    /** The UUID of a player LushStats has recorded under this name, if any. Blocks; call off-thread. */
    public Optional<UUID> knownPlayer(String name) {
        if (!enabled()) return Optional.empty();
        try {
            return worker.submit(() -> {
                try (PreparedStatement statement = connection().prepareStatement(
                        "SELECT uuid FROM lush_players WHERE name_lower = ? ORDER BY updated_at DESC LIMIT 1")) {
                    statement.setString(1, name.toLowerCase(java.util.Locale.ROOT));
                    try (ResultSet rs = statement.executeQuery()) {
                        return rs.next() ? Optional.of(UUID.fromString(rs.getString(1))) : Optional.<UUID>empty();
                    }
                } catch (SQLException e) {
                    logger.warn("Could not read lush_players for '{}': {}", name, e.getMessage());
                    closeQuietly();
                    return Optional.<UUID>empty();
                }
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private int sync() throws SQLException {
        Connection db = connection();
        long highest;
        try (Statement statement = db.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COALESCE(MAX(id), 0) FROM lush_votes")) {
            rs.next();
            highest = rs.getLong(1);
        }
        int copied = 0;
        while (true) {
            List<VoteRepository.VoteRow> rows = votes.after(highest, BATCH);
            if (rows.isEmpty()) return copied;
            try (PreparedStatement insert = db.prepareStatement(
                    "INSERT IGNORE INTO lush_votes (id, uuid, username, service, credited_at) VALUES (?, ?, ?, ?, ?)")) {
                for (VoteRepository.VoteRow row : rows) {
                    insert.setLong(1, row.id());
                    insert.setString(2, row.uuid());
                    insert.setString(3, row.username());
                    insert.setString(4, row.service());
                    insert.setLong(5, row.creditedAt());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            copied += rows.size();
            highest = rows.get(rows.size() - 1).id();
        }
    }

    private Connection connection() throws SQLException {
        if (connection != null && connection.isValid(3)) return connection;
        closeQuietly();
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("The MariaDB driver is missing from the jar", e);
        }
        connection = DriverManager.getConnection("jdbc:mariadb://" + settings.host() + ":" + settings.port() + "/"
                + settings.name() + "?connectTimeout=5000&socketTimeout=15000&tcpKeepAlive=true",
                settings.user(), settings.password());
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS lush_votes (
                        id BIGINT NOT NULL,
                        uuid CHAR(36) NOT NULL,
                        username VARCHAR(32) NOT NULL,
                        service VARCHAR(64) NOT NULL,
                        credited_at BIGINT NOT NULL,
                        PRIMARY KEY (id),
                        KEY idx_votes_uuid (uuid),
                        KEY idx_votes_time (credited_at)
                    ) ENGINE=InnoDB
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS lush_players (
                        uuid CHAR(36) NOT NULL,
                        name VARCHAR(32) NOT NULL,
                        name_lower VARCHAR(32) NOT NULL,
                        bedrock TINYINT(1) NOT NULL DEFAULT 0,
                        updated_at BIGINT NOT NULL,
                        PRIMARY KEY (uuid),
                        KEY idx_players_name (name_lower)
                    ) ENGINE=InnoDB
                    """);
        }
        return connection;
    }

    private void closeQuietly() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ignored) {
            // The connection is being replaced either way.
        }
        connection = null;
    }

    public void close() {
        worker.shutdown();
        try {
            worker.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        closeQuietly();
    }
}
