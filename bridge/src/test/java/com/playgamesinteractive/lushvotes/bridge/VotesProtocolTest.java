package com.playgamesinteractive.lushvotes.bridge;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VotesProtocolTest {

    private static final UUID STEVE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void configSyncRoundTrips() throws Exception {
        var original = new VotesProtocol.ConfigSync(List.of("firework", "sound"), "BALL_LARGE", "YELLOW", "ENTITY_PLAYER_LEVELUP");
        byte[] encoded = VotesProtocol.encodeConfigSync(original);

        assertEquals(VotesProtocol.OPCODE_CONFIG_SYNC, VotesProtocol.opcodeOf(encoded));
        assertEquals(original, VotesProtocol.decodeConfigSync(encoded));
    }

    @Test
    void rewardNowRoundTripsIncludingStats() throws Exception {
        var stats = new VotesProtocol.VoteStats(42, 1_700_000_000_000L);
        var original = new VotesProtocol.RewardNow(STEVE, 7L, List.of("eco give Steve 100"), stats);
        byte[] encoded = VotesProtocol.encodeRewardNow(original);

        assertEquals(VotesProtocol.OPCODE_REWARD_NOW, VotesProtocol.opcodeOf(encoded));
        assertEquals(original, VotesProtocol.decodeRewardNow(encoded));
    }

    @Test
    void requestClaimRoundTrips() throws Exception {
        byte[] encoded = VotesProtocol.encodeRequestClaim(STEVE);

        assertEquals(VotesProtocol.OPCODE_REQUEST_CLAIM, VotesProtocol.opcodeOf(encoded));
        assertEquals(STEVE, VotesProtocol.decodeRequestClaim(encoded));
    }

    @Test
    void pendingResponseRoundTripsWithMultipleRewards() throws Exception {
        var stats = new VotesProtocol.VoteStats(3, -1L);
        var rewards = List.of(
                new VotesProtocol.RewardCommands(1L, List.of("eco give Steve 100")),
                new VotesProtocol.RewardCommands(2L, List.of("eco give Steve 100", "broadcast hi")));
        var original = new VotesProtocol.PendingResponse(STEVE, rewards, stats);
        byte[] encoded = VotesProtocol.encodePendingResponse(original);

        assertEquals(VotesProtocol.OPCODE_PENDING_RESPONSE, VotesProtocol.opcodeOf(encoded));
        assertEquals(original, VotesProtocol.decodePendingResponse(encoded));
    }

    @Test
    void pendingResponseRoundTripsWithNoRewards() throws Exception {
        var original = new VotesProtocol.PendingResponse(STEVE, List.of(), new VotesProtocol.VoteStats(0, -1L));
        byte[] encoded = VotesProtocol.encodePendingResponse(original);
        assertEquals(original, VotesProtocol.decodePendingResponse(encoded));
    }

    @Test
    void deliveredAckRoundTrips() throws Exception {
        byte[] encoded = VotesProtocol.encodeDeliveredAck(99L);

        assertEquals(VotesProtocol.OPCODE_DELIVERED_ACK, VotesProtocol.opcodeOf(encoded));
        assertEquals(99L, VotesProtocol.decodeDeliveredAck(encoded));
    }

    @Test
    void requestSyncCarriesOnlyItsOpcode() {
        byte[] encoded = VotesProtocol.encodeRequestSync();
        assertEquals(1, encoded.length);
        assertEquals(VotesProtocol.OPCODE_REQUEST_SYNC, VotesProtocol.opcodeOf(encoded));
    }

    @Test
    void requestPendingCountRoundTrips() throws Exception {
        byte[] encoded = VotesProtocol.encodeRequestPendingCount(STEVE);

        assertEquals(VotesProtocol.OPCODE_REQUEST_PENDING_COUNT, VotesProtocol.opcodeOf(encoded));
        assertEquals(STEVE, VotesProtocol.decodeRequestPendingCount(encoded));
    }

    @Test
    void pendingCountResponseRoundTrips() throws Exception {
        var original = new VotesProtocol.PendingCount(STEVE, 3);
        byte[] encoded = VotesProtocol.encodePendingCountResponse(original);

        assertEquals(VotesProtocol.OPCODE_PENDING_COUNT_RESPONSE, VotesProtocol.opcodeOf(encoded));
        assertEquals(original, VotesProtocol.decodePendingCountResponse(encoded));
    }

    @Test
    void partyProgressRoundTrips() throws Exception {
        var original = new VotesProtocol.PartyProgress(37, 50);
        byte[] encoded = VotesProtocol.encodePartyProgress(original);

        assertEquals(VotesProtocol.OPCODE_PARTY_PROGRESS, VotesProtocol.opcodeOf(encoded));
        assertEquals(original, VotesProtocol.decodePartyProgress(encoded));
    }

    @Test
    void opcodeOfEmptyArrayIsNegativeOne() {
        assertEquals(-1, VotesProtocol.opcodeOf(new byte[0]));
    }
}
