package com.playgamesinteractive.lushvotes.bridge;

import com.playgamesinteractive.lushlicense.paper.PaperLicenseGate;

import com.playgamesinteractive.lushvotes.bridge.action.ActionRunner;
import com.playgamesinteractive.lushvotes.bridge.command.VoteCommand;
import com.playgamesinteractive.lushvotes.bridge.lang.LangManager;
import com.playgamesinteractive.lushvotes.bridge.menu.MenuListener;
import com.playgamesinteractive.lushvotes.bridge.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

/**
 * Backend companion for LushVotes. Owns no policy of its own - config,
 * dedupe, and vote history all live on the proxy and get mirrored down (see
 * VotesChannelListener). This plugin only ever does things a Velocity
 * proxy structurally cannot: run reward commands, show in-world celebration
 * effects, render the /vote GUI, and answer %lushvotes_*% for PlaceholderAPI.
 */
public final class LushVotesBridgePlugin extends JavaPlugin {

    private PaperLicenseGate license;

    private final CelebrationConfigCache configCache = new CelebrationConfigCache();
    private final VoteStatsCache statsCache = new VoteStatsCache();
    private final VotePartyCache partyCache = new VotePartyCache();

    @Override
    public void onEnable() {
        license = PaperLicenseGate.start(this, "LushVotesBridge");
        if (license == null) return;
        Logger logger = getSLF4JLogger();

        LangManager lang = new LangManager(this);
        lang.load();

        RewardExecutor rewardExecutor = new RewardExecutor(this);
        CelebrationEffects celebrationEffects = new CelebrationEffects(this, lang);

        Bukkit.getMessenger().registerIncomingPluginChannel(this, VotesChannel.NAME,
                new VotesChannelListener(this, configCache, statsCache, partyCache, rewardExecutor, celebrationEffects, logger));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, VotesChannel.NAME);

        RewardJoinRequester joinRequester = new RewardJoinRequester(this, configCache, statsCache);
        Bukkit.getPluginManager().registerEvents(joinRequester, this);
        joinRequester.requestConfigSyncFromAnyOnlinePlayer();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new LushVotesExpansion(statsCache, partyCache).register();
        } else {
            logger.warn("PlaceholderAPI isn't installed - %lushvotes_*% won't be available.");
        }

        MenuManager menuManager = new MenuManager(this);
        menuManager.loadAll();
        Bukkit.getPluginManager().registerEvents(new MenuListener(new ActionRunner(this)), this);
        var voteCommand = getCommand("vote");
        if (voteCommand != null) {
            VoteCommand voteCommandExecutor = new VoteCommand(menuManager, this);
            voteCommand.setExecutor(voteCommandExecutor);
            voteCommand.setTabCompleter(voteCommandExecutor);
        }

        logger.info("LushVotesBridge enabled.");
    }

    @Override
    public void onDisable() {
        if (license != null) license.close();
    }
}
