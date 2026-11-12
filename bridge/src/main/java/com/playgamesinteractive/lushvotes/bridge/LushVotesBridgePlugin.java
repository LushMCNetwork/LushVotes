package com.playgamesinteractive.lushvotes.bridge;

import com.playgamesinteractive.lushvotes.bridge.util.VersionInfo;

import com.playgamesinteractive.lushvotes.bridge.action.ActionRunner;
import com.playgamesinteractive.lushvotes.bridge.command.VoteCommand;
import com.playgamesinteractive.lushvotes.bridge.lang.LangManager;
import com.playgamesinteractive.lushvotes.bridge.menu.MenuListener;
import com.playgamesinteractive.lushvotes.bridge.menu.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class LushVotesBridgePlugin extends JavaPlugin {


    private final CelebrationConfigCache configCache = new CelebrationConfigCache();
    private final VoteStatsCache statsCache = new VoteStatsCache();
    private final VotePartyCache partyCache = new VotePartyCache();

    @Override
    public void onEnable() {
        Logger logger = getSLF4JLogger();
        VersionInfo.load(this);

        LangManager lang = new LangManager(this);
        lang.load();

        RewardExecutor rewardExecutor = new RewardExecutor(this);
        CelebrationEffects celebrationEffects = new CelebrationEffects(this, lang);

        Bukkit.getMessenger().registerIncomingPluginChannel(this, VotesChannel.NAME,
                new VotesChannelListener(this, configCache, statsCache, partyCache, rewardExecutor, celebrationEffects, logger));
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, VotesChannel.NAME);

        RewardJoinRequester joinRequester = new RewardJoinRequester(this, configCache, statsCache, logger);
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
    }
}
