package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PendingRewardRepository {

    public static final String PARTY_SERVICE = "VOTE_PARTY";

    private final Database database;
    private final Logger logger;

    public PendingRewardRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public long create(UUID uuid, String username, String service, Instant createdAt, RewardKind kind) {
        String sql = "INSERT INTO pending_rewards(uuid, username, service, created_at, delivered_at, kind) " +
                "VALUES (?, ?, ?, ?, NULL, ?)";
        try (PreparedStatement statement = database.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, username);
            statement.setString(3, service);
            statement.setLong(4, createdAt.toEpochMilli());
            statement.setString(5, kind.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        } catch (SQLException e) {
            logger.error("Failed to queue pending reward for {} / {}", uuid, service, e);
            return -1L;
        }
    }

    public List<PendingReward> undelivered(UUID uuid) {
        String sql = "SELECT id, uuid, username, service, created_at, delivered_at, kind " +
                "FROM pending_rewards WHERE uuid = ? AND delivered_at IS NULL ORDER BY created_at";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                List<PendingReward> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(map(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            logger.error("Failed to read pending rewards for {}", uuid, e);
            return List.of();
        }
    }

    public int undeliveredCount(UUID uuid) {
        String sql = "SELECT COUNT(*) AS total FROM pending_rewards WHERE uuid = ? AND delivered_at IS NULL";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to count pending rewards for {}", uuid, e);
            return 0;
        }
    }

    public void markDelivered(long id, Instant deliveredAt) {
        String sql = "UPDATE pending_rewards SET delivered_at = ? WHERE id = ? AND delivered_at IS NULL";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, deliveredAt.toEpochMilli());
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to mark pending reward {} delivered", id, e);
        }
    }

    private PendingReward map(ResultSet rs) throws SQLException {
        long deliveredAtMillis = rs.getLong("delivered_at");
        Instant deliveredAt = rs.wasNull() ? null : Instant.ofEpochMilli(deliveredAtMillis);
        RewardKind kind = RewardKind.valueOf(rs.getString("kind"));
        return new PendingReward(rs.getLong("id"), UUID.fromString(rs.getString("uuid")), rs.getString("username"),
                rs.getString("service"), Instant.ofEpochMilli(rs.getLong("created_at")), deliveredAt, kind);
    }
}
