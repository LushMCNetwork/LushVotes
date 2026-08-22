package com.playgamesinteractive.lushvotes.vote;

import com.playgamesinteractive.lushvotes.config.ConfigManager;
import com.playgamesinteractive.lushvotes.mojang.MojangLookup;
import com.playgamesinteractive.lushvotes.mojang.MojangResolver;
import com.playgamesinteractive.lushvotes.storage.Database;
import com.playgamesinteractive.lushvotes.storage.PendingReward;
import com.playgamesinteractive.lushvotes.storage.PendingRewardRepository;
import com.playgamesinteractive.lushvotes.storage.UuidCacheRepository;
import com.playgamesinteractive.lushvotes.storage.VotePartyRepository;
import com.playgamesinteractive.lushvotes.storage.VoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoteServiceTest.class);
    private static final UUID STEVE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private Database database;
    private VoteRepository voteRepository;
    private PendingRewardRepository pendingRewardRepository;
    private VotePartyRepository votePartyRepository;
    private final List<PendingReward> delivered = new ArrayList<>();
    private final List<int[]> progressPushes = new ArrayList<>();
    private int partyTriggerCount = 0;
    private long[] nowMillis = {Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()};

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        database = new Database(tempDir, LOGGER);
        database.open();
        voteRepository = new VoteRepository(database, LOGGER);
        pendingRewardRepository = new PendingRewardRepository(database, LOGGER);
        votePartyRepository = new VotePartyRepository(database, LOGGER);
    }

    @AfterEach
    void tearDown() {
        database.close();
    }

    private VoteService serviceFor(Path dataDirectory) {
        return serviceFor(dataDirectory, null);
    }

    /** partyTarget null means "use the bundled default (50)". */
    private VoteService serviceFor(Path dataDirectory, Integer partyTarget) {
        if (partyTarget != null) {
            writeConfigWithPartyTarget(dataDirectory, partyTarget);
        }
        ConfigManager configManager = new ConfigManager(dataDirectory, LOGGER);
        configManager.load();
        MojangLookup lookup = username -> CompletableFuture.completedFuture(Optional.of(STEVE));
        UuidCacheRepository uuidCache = new UuidCacheRepository(database, LOGGER);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(nowMillis[0]), ZoneOffset.UTC);
        MojangResolver resolver = new MojangResolver(lookup, uuidCache, clock, LOGGER);
        VotePartyDispatcher partyDispatcher = new VotePartyDispatcher() {
            @Override
            public void pushProgress(int current, int target) {
                progressPushes.add(new int[]{current, target});
            }

            @Override
            public void triggerPartyReward() {
                partyTriggerCount++;
            }
        };
        return new VoteService(resolver, voteRepository, pendingRewardRepository, votePartyRepository,
                configManager, clock, delivered::add, partyDispatcher, LOGGER);
    }

    private void writeConfigWithPartyTarget(Path dataDirectory, int target) {
        try {
            Files.createDirectories(dataDirectory);
            Files.writeString(dataDirectory.resolve("config.yml"),
                    "vote-party:\n  target: " + target + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CreditResult await(CompletableFuture<CreditResult> future) throws ExecutionException, InterruptedException {
        return future.get();
    }

    @Test
    void firstVoteIsCreditedAndQueuesAPendingReward(@TempDir Path tempDir) throws Exception {
        CreditResult result = await(serviceFor(tempDir).processVote("Steve", "PlanetMinecraft"));

        assertTrue(result.credited());
        assertEquals(STEVE, result.uuid());
        assertEquals(1, delivered.size());
        assertEquals("PlanetMinecraft", delivered.get(0).service());
    }

    @Test
    void secondVoteInsideTheWindowIsRejectedAsDuplicate(@TempDir Path tempDir) throws Exception {
        VoteService service = serviceFor(tempDir);
        await(service.processVote("Steve", "PlanetMinecraft"));

        nowMillis[0] += 1000; // a second later, still well inside the default 20h window
        CreditResult second = await(serviceFor(tempDir).processVote("Steve", "PlanetMinecraft"));

        assertFalse(second.credited());
        assertEquals("duplicate", second.reason());
        assertEquals(1, delivered.size()); // no second delivery attempt
    }

    @Test
    void voteAfterTheWindowElapsesIsCreditedAgain(@TempDir Path tempDir) throws Exception {
        await(serviceFor(tempDir).processVote("Steve", "PlanetMinecraft"));

        nowMillis[0] += 21L * 60 * 60 * 1000; // 21 hours later, past the default 20h window
        CreditResult second = await(serviceFor(tempDir).processVote("Steve", "PlanetMinecraft"));

        assertTrue(second.credited());
        assertEquals(2, delivered.size());
    }

    @Test
    void adminCreditBypassesDedupeInsideTheWindow(@TempDir Path tempDir) throws Exception {
        VoteService service = serviceFor(tempDir);
        await(service.processVote("Steve", "PlanetMinecraft"));

        CreditResult manual = await(serviceFor(tempDir).creditManual("Steve", "PlanetMinecraft"));

        assertTrue(manual.credited());
        assertEquals(2, delivered.size());
    }

    @Test
    void aDifferentServiceIsNotDedupedAgainstTheFirst(@TempDir Path tempDir) throws Exception {
        VoteService service = serviceFor(tempDir);
        await(service.processVote("Steve", "PlanetMinecraft"));

        CreditResult second = await(serviceFor(tempDir).processVote("Steve", "Minecraft-MP"));

        assertTrue(second.credited());
        assertEquals(2, delivered.size());
    }

    @Test
    void everyCreditedVotePushesPartyProgress(@TempDir Path tempDir) throws Exception {
        await(serviceFor(tempDir, 50).processVote("Steve", "PlanetMinecraft"));

        assertEquals(1, progressPushes.size());
        assertEquals(1, progressPushes.get(0)[0]);
        assertEquals(50, progressPushes.get(0)[1]);
        assertEquals(0, partyTriggerCount);
    }

    @Test
    void reachingTheTargetTriggersThePartyAndResetsTheCounter(@TempDir Path tempDir) throws Exception {
        VoteService service = serviceFor(tempDir, 2);
        await(service.processVote("Steve", "PlanetMinecraft"));
        nowMillis[0] += 1000;
        await(serviceFor(tempDir, 2).processVote("Steve", "Minecraft-MP"));

        assertEquals(1, partyTriggerCount);
        assertEquals(0, votePartyRepository.get());
    }

    @Test
    void testVoteWithPartyFlagFalseDoesNotAdvanceTheCounter(@TempDir Path tempDir) throws Exception {
        CreditResult result = await(serviceFor(tempDir, 2).creditTestVote("Steve", "PlanetMinecraft", false));

        assertTrue(result.credited());
        assertEquals(0, votePartyRepository.get());
        assertTrue(progressPushes.isEmpty());
    }

    @Test
    void testVoteWithPartyFlagTrueAdvancesTheCounter(@TempDir Path tempDir) throws Exception {
        await(serviceFor(tempDir, 2).creditTestVote("Steve", "PlanetMinecraft", true));

        assertEquals(1, votePartyRepository.get());
        assertEquals(1, progressPushes.size());
    }

    @Test
    void testVoteBypassesDedupeRegardlessOfPartyFlag(@TempDir Path tempDir) throws Exception {
        VoteService service = serviceFor(tempDir);
        await(service.processVote("Steve", "PlanetMinecraft"));

        CreditResult testVote = await(serviceFor(tempDir).creditTestVote("Steve", "PlanetMinecraft", false));

        assertTrue(testVote.credited());
    }
}
