package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Vote history. */
public final class VoteRepository {

    private final Database database;
    private final Logger logger;
    private volatile Runnable afterRecord = () -> { };

    /** One vote, as copied to the network database. */
    public record VoteRow(long id, String uuid, String username, String service, long creditedAt) {
    }

    public VoteRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

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
            return;
        }
        afterRecord.run();
    }

    /** Runs after each recorded vote; used to copy it to the network database. */
    public void afterRecord(Runnable action) {
        this.afterRecord = action;
    }

    /** Up to {@code limit} votes with an id above {@code id}, oldest first. */
    public List<VoteRow> after(long id, int limit) {
        String sql = "SELECT id, uuid, username, service, credited_at FROM votes WHERE id > ? ORDER BY id LIMIT ?";
        List<VoteRow> rows = new ArrayList<>();
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setInt(2, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new VoteRow(rs.getLong("id"), rs.getString("uuid"), rs.getString("username"),
                            rs.getString("service"), rs.getLong("credited_at")));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to read votes after id {}", id, e);
        }
        return rows;
    }
}
