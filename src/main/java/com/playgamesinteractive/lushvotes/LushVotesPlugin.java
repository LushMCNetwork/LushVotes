package com.playgamesinteractive.lushvotes;

import com.google.inject.Inject;
import com.playgamesinteractive.lushvotes.bridge.VotesBridgeChannel;
import com.playgamesinteractive.lushvotes.bridge.VotesBridgeListener;
import com.playgamesinteractive.lushvotes.command.LushVotesCommand;
import com.playgamesinteractive.lushvotes.config.ConfigManager;
import com.playgamesinteractive.lushvotes.mojang.HttpMojangLookup;
import com.playgamesinteractive.lushvotes.mojang.MojangResolver;
import com.playgamesinteractive.lushvotes.storage.Database;
import com.playgamesinteractive.lushvotes.storage.PendingRewardRepository;
import com.playgamesinteractive.lushvotes.storage.UuidCacheRepository;
import com.playgamesinteractive.lushvotes.storage.VotePartyRepository;
import com.playgamesinteractive.lushvotes.storage.VoteRepository;
import com.playgamesinteractive.lushvotes.vote.VoteListener;
import com.playgamesinteractive.lushvotes.vote.VoteService;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Clock;

@Plugin(
        id = "lushvotes",
        name = "LushVotes",
        version = "1.0.0",
        description = "Hooks NuVotifier-Velocity's own event bus directly and credits votes network-wide.",
        authors = {"LushMC"}
)
public final class LushVotesPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private Database database;

    @Inject
    public LushVotesPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        ConfigManager configManager = new ConfigManager(dataDirectory, logger);
        configManager.load();

        database = new Database(dataDirectory, logger);
        database.open();

        UuidCacheRepository uuidCache = new UuidCacheRepository(database, logger);
        VoteRepository voteRepository = new VoteRepository(database, logger);
        PendingRewardRepository pendingRewardRepository = new PendingRewardRepository(database, logger);
        VotePartyRepository votePartyRepository = new VotePartyRepository(database, logger);

        MojangResolver mojangResolver = new MojangResolver(new HttpMojangLookup(logger), uuidCache, Clock.systemUTC(), logger);

        server.getChannelRegistrar().register(VotesBridgeChannel.IDENTIFIER);
        VotesBridgeListener bridgeListener = new VotesBridgeListener(server, configManager, pendingRewardRepository,
                voteRepository, votePartyRepository, Clock.systemUTC(), logger);
        server.getEventManager().register(this, bridgeListener);

        VoteService voteService = new VoteService(mojangResolver, voteRepository, pendingRewardRepository,
                votePartyRepository, configManager, Clock.systemUTC(), bridgeListener, bridgeListener, logger);
        server.getEventManager().register(this, new VoteListener(voteService, logger));

        server.getCommandManager().register("lushvotes",
                new LushVotesCommand(configManager, bridgeListener, voteService, votePartyRepository, voteRepository, mojangResolver));

        logger.info("LushVotes enabled - listening on NuVotifier-Velocity's event bus.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (database != null) {
            database.close();
        }
    }
}
