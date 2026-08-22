package com.playgamesinteractive.lushvotes.bridge;

import com.playgamesinteractive.lushvotes.config.ConfigManager;
import com.playgamesinteractive.lushvotes.reward.RewardCommandRenderer;
import com.playgamesinteractive.lushvotes.storage.PendingReward;
import com.playgamesinteractive.lushvotes.storage.PendingRewardRepository;
import com.playgamesinteractive.lushvotes.storage.RewardKind;
import com.playgamesinteractive.lushvotes.storage.VotePartyRepository;
import com.playgamesinteractive.lushvotes.storage.VoteRepository;
import com.playgamesinteractive.lushvotes.vote.RewardDispatcher;
import com.playgamesinteractive.lushvotes.vote.VotePartyDispatcher;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Proxy side of {@link VotesBridgeChannel}. Pushes celebration/party config
 * to a freshly (re)connected backend, answers a backend's claim/count
 * requests (auth-checked the same way as LushRelay's
 * PunishBridgeRequestListener - the request only makes sense if that UUID
 * is really a player currently connected through the requesting backend),
 * records delivery acks, and implements both {@link RewardDispatcher} (a
 * single vote's immediate-delivery path) and {@link VotePartyDispatcher}
 * (rewarding every online player once the party target is hit).
 * <p>
 * Plugin messages to a backend ride on an actual player's connection (see
 * {@code ServerConnection#sendPluginMessage}) - there's no "send to a
 * server" independent of any player, same gotcha documented on
 * VanishBridgeListener.
 */
public final class VotesBridgeListener implements RewardDispatcher, VotePartyDispatcher {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final PendingRewardRepository pendingRewards;
    private final VoteRepository voteRepository;
    private final VotePartyRepository votePartyRepository;
    private final Clock clock;
    private final Logger logger;

    public VotesBridgeListener(ProxyServer server, ConfigManager configManager, PendingRewardRepository pendingRewards,
                                VoteRepository voteRepository, VotePartyRepository votePartyRepository,
                                Clock clock, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.pendingRewards = pendingRewards;
        this.voteRepository = voteRepository;
        this.votePartyRepository = votePartyRepository;
        this.clock = clock;
        this.logger = logger;
    }

    @Subscribe
    public void onServerPostConnect(ServerPostConnectEvent event) {
        // Only push once per backend (the first player to land there since it came up),
        // not on every join - same throttle as VanishBridgeListener.
        if (event.getPlayer().getCurrentServer()
                .map(connection -> connection.getServer().getPlayersConnected().size()).orElse(0) != 1) {
            return;
        }
        event.getPlayer().getCurrentServer().ifPresent(connection -> {
            sendConfigSync(connection);
            sendPartyProgress(connection);
        });
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!VotesBridgeChannel.IDENTIFIER.equals(event.getIdentifier())) {
            return;
        }
        byte[] data = event.getData();
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection connection)) {
            return;
        }
        try {
            switch (VotesProtocol.opcodeOf(data)) {
                case VotesProtocol.OPCODE_REQUEST_SYNC -> {
                    sendConfigSync(connection);
                    sendPartyProgress(connection);
                }
                case VotesProtocol.OPCODE_REQUEST_CLAIM -> handleRequestClaim(connection, data);
                case VotesProtocol.OPCODE_REQUEST_PENDING_COUNT -> handleRequestPendingCount(connection, data);
                case VotesProtocol.OPCODE_DELIVERED_ACK -> handleDeliveredAck(data);
                default -> logger.warn("Unknown LushVotes bridge opcode {} from {}",
                        VotesProtocol.opcodeOf(data), connection.getServerInfo().getName());
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.warn("Rejected malformed LushVotes bridge message from {}", connection.getServerInfo().getName(), e);
        }
    }

    /** Called by /lushvotes admin reload so already-connected backends pick up edits without a restart. */
    public void broadcastConfigToAllBackends() {
        forEachConnectedBackend(this::sendConfigSync);
    }

    /** Called after every credited vote (see VoteService) - keeps %lushvotes_party_*% fresh everywhere, not just where the vote landed. */
    @Override
    public void pushProgress(int current, int target) {
        forEachConnectedBackend(connection -> send(connection, VotesProtocol.encodePartyProgress(
                new VotesProtocol.PartyProgress(current, target))));
    }

    @Override
    public void triggerPartyReward() {
        Instant now = Instant.now(clock);
        for (Player player : server.getAllPlayers()) {
            long id = pendingRewards.create(player.getUniqueId(), player.getUsername(),
                    PendingRewardRepository.PARTY_SERVICE, now, RewardKind.PARTY);
            if (id < 0) {
                continue;
            }
            PendingReward reward = new PendingReward(id, player.getUniqueId(), player.getUsername(),
                    PendingRewardRepository.PARTY_SERVICE, now, null, RewardKind.PARTY);
            deliverNow(reward);
        }
    }

    @Override
    public void deliverNow(PendingReward reward) {
        server.getPlayer(reward.uuid()).flatMap(Player::getCurrentServer).ifPresent(connection -> {
            List<String> commands = renderCommands(reward);
            byte[] payload = VotesProtocol.encodeRewardNow(new VotesProtocol.RewardNow(reward.uuid(), reward.id(), commands, statsFor(reward.uuid())));
            send(connection, payload);
        });
    }

    private List<String> renderCommands(PendingReward reward) {
        List<String> templates = reward.kind() == RewardKind.PARTY
                ? configManager.config().votePartyCommands()
                : configManager.config().rewardCommands();
        return RewardCommandRenderer.render(templates, reward.username());
    }

    private void handleRequestClaim(ServerConnection connection, byte[] data) throws IOException {
        UUID uuid = VotesProtocol.decodeRequestClaim(data);
        if (!authorized(connection, uuid)) {
            return;
        }
        List<VotesProtocol.RewardCommands> rewards = pendingRewards.undelivered(uuid).stream()
                .map(reward -> new VotesProtocol.RewardCommands(reward.id(), renderCommands(reward)))
                .toList();
        send(connection, VotesProtocol.encodePendingResponse(new VotesProtocol.PendingResponse(uuid, rewards, statsFor(uuid))));
    }

    private void handleRequestPendingCount(ServerConnection connection, byte[] data) throws IOException {
        UUID uuid = VotesProtocol.decodeRequestPendingCount(data);
        if (!authorized(connection, uuid)) {
            return;
        }
        int count = pendingRewards.undeliveredCount(uuid);
        send(connection, VotesProtocol.encodePendingCountResponse(new VotesProtocol.PendingCount(uuid, count)));
    }

    private boolean authorized(ServerConnection connection, UUID uuid) {
        boolean authorized = connection.getServer().getPlayersConnected().stream()
                .anyMatch(player -> player.getUniqueId().equals(uuid));
        if (!authorized) {
            logger.warn("Rejected a LushVotes request for {} from {} - not a connected player there",
                    uuid, connection.getServerInfo().getName());
        }
        return authorized;
    }

    private void handleDeliveredAck(byte[] data) throws IOException {
        long pendingRewardId = VotesProtocol.decodeDeliveredAck(data);
        pendingRewards.markDelivered(pendingRewardId, Instant.now(clock));
    }

    private VotesProtocol.VoteStats statsFor(UUID uuid) {
        long total = voteRepository.totalVotes(uuid);
        long lastVoteAtEpochMillis = voteRepository.lastVoteAt(uuid).map(Instant::toEpochMilli).orElse(-1L);
        return new VotesProtocol.VoteStats(total, lastVoteAtEpochMillis);
    }

    private void sendConfigSync(ServerConnection connection) {
        var config = configManager.config();
        byte[] payload = VotesProtocol.encodeConfigSync(new VotesProtocol.ConfigSync(
                config.celebrationEffects(), config.fireworkType(), config.fireworkColor(), config.celebrationSound()));
        send(connection, payload);
    }

    private void sendPartyProgress(ServerConnection connection) {
        send(connection, VotesProtocol.encodePartyProgress(
                new VotesProtocol.PartyProgress(votePartyRepository.get(), configManager.config().votePartyTarget())));
    }

    private void forEachConnectedBackend(java.util.function.Consumer<ServerConnection> action) {
        for (RegisteredServer registeredServer : server.getAllServers()) {
            registeredServer.getPlayersConnected().stream().findFirst()
                    .flatMap(Player::getCurrentServer)
                    .ifPresent(action);
        }
    }

    private void send(ServerConnection connection, byte[] payload) {
        connection.sendPluginMessage(VotesBridgeChannel.IDENTIFIER, payload);
    }
}
