package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class VotePartyRepository {

    private final Database database;
    private final Logger logger;

    public VotePartyRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

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
