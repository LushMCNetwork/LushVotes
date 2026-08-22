package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Vote history - the dedupe source of truth for the 20h (configurable) window. */
public final class VoteRepository {

    private final Database database;
    private final Logger logger;

    public VoteRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public Optional<Instant> lastCreditedAt(UUID uuid, String service) {
        String sql = "SELECT MAX(credited_at) AS latest FROM votes WHERE uuid = ? AND service = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, service);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getObject("latest") == null) {
                    return Optional.empty();
                }
                return Optional.of(Instant.ofEpochMilli(rs.getLong("latest")));
            }
        } catch (SQLException e) {
            logger.error("Failed to read vote history for {} / {}", uuid, service, e);
            return Optional.empty();
        }
    }

    /** Backs %lushvotes_total%/%lushvotes_last% - mirrored to the bridge with each reward delivery message. */
    public long totalVotes(UUID uuid) {
        String sql = "SELECT COUNT(*) AS total FROM votes WHERE uuid = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong("total") : 0L;
            }
        } catch (SQLException e) {
            logger.error("Failed to count votes for {}", uuid, e);
            return 0L;
        }
    }

    public Optional<Instant> lastVoteAt(UUID uuid) {
        String sql = "SELECT MAX(credited_at) AS latest FROM votes WHERE uuid = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || rs.getObject("latest") == null) {
                    return Optional.empty();
                }
                return Optional.of(Instant.ofEpochMilli(rs.getLong("latest")));
            }
        } catch (SQLException e) {
            logger.error("Failed to read last vote time for {}", uuid, e);
            return Optional.empty();
        }
    }

    public void record(UUID uuid, String username, String service, Instant creditedAt) {
        String sql = "INSERT INTO votes(uuid, username, service, credited_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, username);
            statement.setString(3, service);
            statement.setLong(4, creditedAt.toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record vote for {} / {}", uuid, service, e);
        }
    }
}
