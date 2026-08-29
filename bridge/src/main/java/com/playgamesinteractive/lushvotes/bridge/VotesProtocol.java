package com.playgamesinteractive.lushvotes.bridge;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Byte-level codec for every message that crosses {@link VotesBridgeChannel}.
 * Pure encode/decode, no side effects, so both ends can round-trip test it
 * independently - same opcode-byte-first framing convention as LushRelay's
 * vanish/punish channels (see VanishBridgeListener), hand-duplicated on the
 * bridge module's own copy of this class since there's no shared Maven
 * module between the two artifacts (matching LushRelay's own precedent).
 */
public final class VotesProtocol {

    public static final byte OPCODE_CONFIG_SYNC = 0;
    public static final byte OPCODE_REWARD_NOW = 1;
    /** bridge -> proxy, sent by /vote claim - "give me everything queued and mark it delivered". */
    public static final byte OPCODE_REQUEST_CLAIM = 2;
    public static final byte OPCODE_PENDING_RESPONSE = 3;
    public static final byte OPCODE_DELIVERED_ACK = 4;
    public static final byte OPCODE_REQUEST_SYNC = 5;
    /** bridge -> proxy, sent on join - "how many rewards are waiting?", informational only, nothing is executed or acked. */
    public static final byte OPCODE_REQUEST_PENDING_COUNT = 6;
    public static final byte OPCODE_PENDING_COUNT_RESPONSE = 7;
    /** proxy -> bridge, broadcast to every connected backend after each credited vote, and on REQUEST_SYNC catch-up. */
    public static final byte OPCODE_PARTY_PROGRESS = 8;
    /**
     * bridge -> proxy, sent on join - "what are this player's real vote
     * totals?". Unlike REQUEST_SYNC (config/party, network-wide) and
     * REQUEST_PENDING_COUNT (unclaimed-reward count only), nothing else
     * ever pushes a player's totalVotes/lastVoteAt to a freshly (re)started
     * bridge - VoteStatsCache was otherwise only ever touched by REWARD_NOW
     * and PENDING_RESPONSE, both tied to an actual vote/claim happening
     * *after* the cache came up, so %lushvotes_total% read 0 for anyone who
     * hadn't voted or /vote claim'd since.
     */
    public static final byte OPCODE_REQUEST_STATS = 9;
    public static final byte OPCODE_STATS_RESPONSE = 10;

    private VotesProtocol() {
    }

    /** fireworkType/fireworkColor/sound are enum names, validated on the bridge (only side with Bukkit) - see LushVotesConfig's doc. */
    public record ConfigSync(List<String> effects, String fireworkType, String fireworkColor, String sound) {
    }

    /** {@code lastVoteAtEpochMillis} is -1 for "never voted" - only meaningful once {@code totalVotes > 0}. */
    public record VoteStats(long totalVotes, long lastVoteAtEpochMillis) {
    }

    /**
     * Stats ride along with both reward-delivery messages (rather than a
     * dedicated opcode) because those are exactly the moments
     * %lushvotes_total%/%lushvotes_last% need refreshing - immediately after
     * a credited vote while online, and on claim. See VotesBridgeListener.
     */
    public record RewardNow(UUID uuid, long pendingRewardId, List<String> commands, VoteStats stats) {
    }

    public record RewardCommands(long pendingRewardId, List<String> commands) {
    }

    public record PendingResponse(UUID uuid, List<RewardCommands> rewards, VoteStats stats) {
    }

    public record PendingCount(UUID uuid, int count) {
    }

    public record PartyProgress(int current, int target) {
    }

    public record StatsResponse(UUID uuid, VoteStats stats) {
    }

    public static byte[] encodeConfigSync(ConfigSync config) {
        return write(OPCODE_CONFIG_SYNC, out -> {
            writeStrings(out, config.effects());
            out.writeUTF(config.fireworkType());
            out.writeUTF(config.fireworkColor());
            out.writeUTF(config.sound());
        });
    }

    public static ConfigSync decodeConfigSync(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            List<String> effects = readStrings(in);
            String fireworkType = in.readUTF();
            String fireworkColor = in.readUTF();
            String sound = in.readUTF();
            return new ConfigSync(effects, fireworkType, fireworkColor, sound);
        }
    }

    public static byte[] encodeRewardNow(RewardNow reward) {
        return write(OPCODE_REWARD_NOW, out -> {
            out.writeUTF(reward.uuid().toString());
            out.writeLong(reward.pendingRewardId());
            writeStrings(out, reward.commands());
            writeStats(out, reward.stats());
        });
    }

    public static RewardNow decodeRewardNow(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            UUID uuid = UUID.fromString(in.readUTF());
            long pendingRewardId = in.readLong();
            List<String> commands = readStrings(in);
            VoteStats stats = readStats(in);
            return new RewardNow(uuid, pendingRewardId, commands, stats);
        }
    }

    public static byte[] encodeRequestClaim(UUID uuid) {
        return write(OPCODE_REQUEST_CLAIM, out -> out.writeUTF(uuid.toString()));
    }

    public static UUID decodeRequestClaim(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            return UUID.fromString(in.readUTF());
        }
    }

    public static byte[] encodePendingResponse(PendingResponse response) {
        return write(OPCODE_PENDING_RESPONSE, out -> {
            out.writeUTF(response.uuid().toString());
            out.writeInt(response.rewards().size());
            for (RewardCommands reward : response.rewards()) {
                out.writeLong(reward.pendingRewardId());
                writeStrings(out, reward.commands());
            }
            writeStats(out, response.stats());
        });
    }

    public static PendingResponse decodePendingResponse(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            UUID uuid = UUID.fromString(in.readUTF());
            int count = in.readInt();
            List<RewardCommands> rewards = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                long pendingRewardId = in.readLong();
                rewards.add(new RewardCommands(pendingRewardId, readStrings(in)));
            }
            VoteStats stats = readStats(in);
            return new PendingResponse(uuid, rewards, stats);
        }
    }

    public static byte[] encodeDeliveredAck(long pendingRewardId) {
        return write(OPCODE_DELIVERED_ACK, out -> out.writeLong(pendingRewardId));
    }

    public static long decodeDeliveredAck(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            return in.readLong();
        }
    }

    public static byte[] encodeRequestSync() {
        return write(OPCODE_REQUEST_SYNC, out -> {
        });
    }

    public static byte[] encodeRequestPendingCount(UUID uuid) {
        return write(OPCODE_REQUEST_PENDING_COUNT, out -> out.writeUTF(uuid.toString()));
    }

    public static UUID decodeRequestPendingCount(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            return UUID.fromString(in.readUTF());
        }
    }

    public static byte[] encodePendingCountResponse(PendingCount count) {
        return write(OPCODE_PENDING_COUNT_RESPONSE, out -> {
            out.writeUTF(count.uuid().toString());
            out.writeInt(count.count());
        });
    }

    public static PendingCount decodePendingCountResponse(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            return new PendingCount(UUID.fromString(in.readUTF()), in.readInt());
        }
    }

    public static byte[] encodePartyProgress(PartyProgress progress) {
        return write(OPCODE_PARTY_PROGRESS, out -> {
            out.writeInt(progress.current());
            out.writeInt(progress.target());
        });
    }

    public static PartyProgress decodePartyProgress(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            return new PartyProgress(in.readInt(), in.readInt());
        }
    }

    public static byte[] encodeRequestStats(UUID uuid) {
        return write(OPCODE_REQUEST_STATS, out -> out.writeUTF(uuid.toString()));
    }

    public static UUID decodeRequestStats(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            return UUID.fromString(in.readUTF());
        }
    }

    public static byte[] encodeStatsResponse(StatsResponse response) {
        return write(OPCODE_STATS_RESPONSE, out -> {
            out.writeUTF(response.uuid().toString());
            writeStats(out, response.stats());
        });
    }

    public static StatsResponse decodeStatsResponse(byte[] data) throws IOException {
        try (DataInputStream in = payload(data)) {
            UUID uuid = UUID.fromString(in.readUTF());
            VoteStats stats = readStats(in);
            return new StatsResponse(uuid, stats);
        }
    }

    public static byte opcodeOf(byte[] data) {
        return data.length == 0 ? -1 : data[0];
    }

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] write(byte opcode, Writer writer) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(opcode);
            writer.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e); // in-memory stream, never actually throws
        }
        return bytes.toByteArray();
    }

    private static DataInputStream payload(byte[] data) {
        return new DataInputStream(new ByteArrayInputStream(data, 1, data.length - 1));
    }

    private static void writeStrings(DataOutputStream out, List<String> values) throws IOException {
        out.writeInt(values.size());
        for (String value : values) {
            out.writeUTF(value);
        }
    }

    private static List<String> readStrings(DataInputStream in) throws IOException {
        int count = in.readInt();
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(in.readUTF());
        }
        return values;
    }

    private static void writeStats(DataOutputStream out, VoteStats stats) throws IOException {
        out.writeLong(stats.totalVotes());
        out.writeLong(stats.lastVoteAtEpochMillis());
    }

    private static VoteStats readStats(DataInputStream in) throws IOException {
        return new VoteStats(in.readLong(), in.readLong());
    }
}
