package com.playgamesinteractive.lushvotes.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingRewardRepositoryTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PendingRewardRepositoryTest.class);
    private static final UUID STEVE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Database database;
    private PendingRewardRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        database = new Database(tempDir, LOGGER);
        database.open();
        repository = new PendingRewardRepository(database, LOGGER);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    @Test
    void newlyCreatedRewardIsUndelivered() {
        long id = repository.create(STEVE, "Steve", "PlanetMinecraft", Instant.now(), RewardKind.VOTE);

        List<PendingReward> undelivered = repository.undelivered(STEVE);

        assertEquals(1, undelivered.size());
        assertEquals(id, undelivered.get(0).id());
        assertTrue(!undelivered.get(0).delivered());
    }

    @Test
    void markingDeliveredRemovesItFromTheUndeliveredList() {
        long id = repository.create(STEVE, "Steve", "PlanetMinecraft", Instant.now(), RewardKind.VOTE);

        repository.markDelivered(id, Instant.now());

        assertEquals(0, repository.undelivered(STEVE).size());
    }

    @Test
    void undeliveredIsScopedToTheGivenUuid() {
        UUID alex = UUID.fromString("22222222-2222-2222-2222-222222222222");
        repository.create(STEVE, "Steve", "PlanetMinecraft", Instant.now(), RewardKind.VOTE);
        repository.create(alex, "Alex", "PlanetMinecraft", Instant.now(), RewardKind.VOTE);

        assertEquals(1, repository.undelivered(STEVE).size());
        assertEquals(1, repository.undelivered(alex).size());
    }

    @Test
    void markingDeliveredTwiceIsHarmless() {
        long id = repository.create(STEVE, "Steve", "PlanetMinecraft", Instant.now(), RewardKind.VOTE);
        repository.markDelivered(id, Instant.now());
        repository.markDelivered(id, Instant.now()); // ack arriving twice must not throw or corrupt state

        assertEquals(0, repository.undelivered(STEVE).size());
    }

    @Test
    void kindRoundTripsThroughStorage() {
        repository.create(STEVE, "Steve", PendingRewardRepository.PARTY_SERVICE, Instant.now(), RewardKind.PARTY);

        PendingReward reward = repository.undelivered(STEVE).get(0);

        assertEquals(RewardKind.PARTY, reward.kind());
        assertEquals(PendingRewardRepository.PARTY_SERVICE, reward.service());
    }

    @Test
    void undeliveredCountMatchesUndeliveredListSize() {
        repository.create(STEVE, "Steve", "PlanetMinecraft", Instant.now(), RewardKind.VOTE);
        repository.create(STEVE, "Steve", "Minecraft-MP", Instant.now(), RewardKind.VOTE);

        assertEquals(2, repository.undeliveredCount(STEVE));
    }
}
