package com.playgamesinteractive.lushvotes.storage;

import org.slf4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Username -> UUID cache backing {@code MojangResolver}, so most votes never hit Mojang at all. */
public final class UuidCacheRepository {

    private final Database database;
    private final Logger logger;

    public UuidCacheRepository(Database database, Logger logger) {
        this.database = database;
        this.logger = logger;
    }

    public Optional<CachedUuid> find(String username) {
        String sql = "SELECT uuid, resolved_at FROM uuid_cache WHERE username = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CachedUuid(UUID.fromString(rs.getString("uuid")),
                        Instant.ofEpochMilli(rs.getLong("resolved_at"))));
            }
        } catch (SQLException e) {
            logger.error("Failed to read uuid_cache for '{}'", username, e);
            return Optional.empty();
        }
    }

    public void put(String username, UUID uuid, Instant resolvedAt) {
        String sql = "INSERT INTO uuid_cache(username, uuid, resolved_at) VALUES (?, ?, ?) " +
                "ON CONFLICT(username) DO UPDATE SET uuid = excluded.uuid, resolved_at = excluded.resolved_at";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, uuid.toString());
            statement.setLong(3, resolvedAt.toEpochMilli());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to cache uuid for '{}'", username, e);
        }
    }
}
