package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Tracks progress toward the network-wide vote party (see the design's
 * vote-party section) as a singleton row (id always 1, seeded by
 * {@link Database#open()}).
 */
public final class VotePartyRepository {

    private final Database database;
    private final Logger logger;

    public VotePartyRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    /**
     * Atomically increments and returns the new counter value. {@code
     * synchronized} rather than relying solely on the single UPDATE
     * statement - two votes landing near-simultaneously both run on async
     * callback threads sharing one JDBC {@code Connection}, and this is the
     * one path where a lost/duplicated increment would double- or under-fire
     * the party (every other repository here is read-mostly or per-row, so
     * a race is far less consequential than it is here).
     */
    public synchronized int incrementAndGet() {
        String sql = "UPDATE vote_party SET counter = counter + 1 WHERE id = 1 RETURNING counter";
        try (PreparedStatement statement = database.connection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt("counter") : 0;
        } catch (SQLException e) {
            logger.error("Failed to increment vote party counter", e);
            return 0;
        }
    }

    public int get() {
        String sql = "SELECT counter FROM vote_party WHERE id = 1";
        try (PreparedStatement statement = database.connection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt("counter") : 0;
        } catch (SQLException e) {
            logger.error("Failed to read vote party counter", e);
            return 0;
        }
    }

    public synchronized void set(int value) {
        String sql = "UPDATE vote_party SET counter = ? WHERE id = 1";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setInt(1, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set vote party counter", e);
        }
    }

    public void reset() {
        set(0);
    }
}
