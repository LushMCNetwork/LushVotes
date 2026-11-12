package com.playgamesinteractive.lushvotes.bridge;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class LushVotesExpansion extends PlaceholderExpansion {

    private static final DateTimeFormatter LAST_VOTE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final VoteStatsCache statsCache;
    private final VotePartyCache partyCache;

    LushVotesExpansion(VoteStatsCache statsCache, VotePartyCache partyCache) {
        this.statsCache = statsCache;
        this.partyCache = partyCache;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "lushvotes";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "PlayGamesInteractive";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        switch (key) {
            case "party_current":
                return String.valueOf(partyCache.current());
            case "party_target":
                return String.valueOf(partyCache.target());
            case "party_remaining":
                return String.valueOf(partyCache.remaining());
            default:
                break;
        }
        if (player == null) {
            return "";
        }
        VoteStatsCache.Stats stats = statsCache.get(player.getUniqueId());
        return switch (key) {
            case "loaded" -> String.valueOf(statsCache.loaded(player.getUniqueId()));
            case "total" -> String.valueOf(stats.totalVotes());
            case "total_formatted" -> String.format(Locale.US, "%,d", stats.totalVotes());
            case "last" -> stats.lastVoteAtEpochMillis() < 0 ? "never"
                    : LAST_VOTE_FORMAT.format(Instant.ofEpochMilli(stats.lastVoteAtEpochMillis()));
            default -> null;
        };
    }
}
