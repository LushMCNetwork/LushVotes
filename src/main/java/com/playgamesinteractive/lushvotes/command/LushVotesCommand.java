package com.playgamesinteractive.lushvotes.command;

import com.playgamesinteractive.lushvotes.bridge.VotesBridgeListener;
import com.playgamesinteractive.lushvotes.config.ConfigManager;
import com.playgamesinteractive.lushvotes.mojang.MojangResolver;
import com.playgamesinteractive.lushvotes.storage.VotePartyRepository;
import com.playgamesinteractive.lushvotes.storage.VoteRepository;
import com.playgamesinteractive.lushvotes.util.ColorText;
import com.playgamesinteractive.lushvotes.util.VersionInfo;
import com.playgamesinteractive.lushvotes.vote.CreditResult;
import com.playgamesinteractive.lushvotes.vote.VoteService;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /lushvotes admin <subcommand>} - everything admin-facing lives
 * here on the proxy (not split across a bridge-side "/vote admin" surface
 * too) since all the actual data - vote history, dedupe, the party counter -
 * lives in LushVotes' own SQLite store, not on any backend.
 */
public final class LushVotesCommand implements SimpleCommand {

    private static final String PERMISSION = "lushvotes.admin";
    private static final DateTimeFormatter LAST_VOTE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private static final List<String> ADMIN_SUBCOMMANDS =
            List.of("reload", "credit", "testvote", "check", "party", "version");
    private static final List<String> PARTY_SUBCOMMANDS = List.of("status", "reset", "set");
    private static final List<String> BOOLEAN_SUGGESTIONS = List.of("true", "false");

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final VotesBridgeListener bridgeListener;
    private final VoteService voteService;
    private final VotePartyRepository votePartyRepository;
    private final VoteRepository voteRepository;
    private final MojangResolver mojangResolver;

    public LushVotesCommand(ProxyServer server, ConfigManager configManager, VotesBridgeListener bridgeListener,
                             VoteService voteService, VotePartyRepository votePartyRepository,
                             VoteRepository voteRepository, MojangResolver mojangResolver) {
        this.server = server;
        this.configManager = configManager;
        this.bridgeListener = bridgeListener;
        this.voteService = voteService;
        this.votePartyRepository = votePartyRepository;
        this.voteRepository = voteRepository;
        this.mojangResolver = mojangResolver;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || !args[0].equalsIgnoreCase("admin")) {
            reply(invocation, "admin.usage");
            return;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (rest.length == 0) {
            reply(invocation, "admin.usage");
            return;
        }
        switch (rest[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(invocation);
            case "credit" -> handleCredit(invocation, rest);
            case "testvote" -> handleTestVote(invocation, rest);
            case "check" -> handleCheck(invocation, rest);
            case "party" -> handleParty(invocation, rest);
            case "version" -> reply(invocation, "admin.version",
                    "commit", VersionInfo.commit(), "built", VersionInfo.built());
            default -> reply(invocation, "admin.unknown");
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length <= 1) {
            return filter(List.of("admin"), args.length == 0 ? "" : args[0]);
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (rest.length == 1) {
            return filter(ADMIN_SUBCOMMANDS, rest[0]);
        }
        return switch (rest[0].toLowerCase(Locale.ROOT)) {
            case "credit" -> rest.length == 2 ? filter(onlinePlayerNames(), rest[1]) : List.of();
            case "testvote" -> suggestTestVote(rest);
            case "check" -> rest.length == 2 ? filter(onlinePlayerNames(), rest[1]) : List.of();
            case "party" -> rest.length == 2 ? filter(PARTY_SUBCOMMANDS, rest[1]) : List.of();
            default -> List.of();
        };
    }

    private List<String> suggestTestVote(String[] rest) {
        if (rest.length == 2) {
            return filter(onlinePlayerNames(), rest[1]);
        }
        if (rest.length == 4) {
            return filter(BOOLEAN_SUGGESTIONS, rest[3]);
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return server.getAllPlayers().stream().map(Player::getUsername).toList();
    }

    /** Package-private (not private) so LushVotesCommandTest can exercise it directly. */
    static List<String> filter(List<String> options, String prefix) {
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }

    private void handleReload(Invocation invocation) {
        configManager.load();
        bridgeListener.broadcastConfigToAllBackends();
        reply(invocation, "admin.reload.success");
    }

    private void handleCredit(Invocation invocation, String[] rest) {
        if (rest.length < 3) {
            reply(invocation, "admin.credit.usage");
            return;
        }
        String player = rest[1];
        String service = rest[2];
        voteService.creditManual(player, service).thenAccept(result ->
                replyCreditLikeResult(invocation, result, "admin.credit.success", "admin.credit.player-not-found", player, service));
    }

    private void handleTestVote(Invocation invocation, String[] rest) {
        if (rest.length < 4) {
            reply(invocation, "admin.testvote.usage");
            return;
        }
        String player = rest[1];
        String service = rest[2];
        boolean countTowardParty = Boolean.parseBoolean(rest[3]);
        voteService.creditTestVote(player, service, countTowardParty).thenAccept(result -> {
            if (!result.credited()) {
                reply(invocation, "admin.testvote.player-not-found", "player", player);
                return;
            }
            reply(invocation, "admin.testvote.success", "player", player, "service", service, "counted", String.valueOf(countTowardParty));
        });
    }

    private void handleCheck(Invocation invocation, String[] rest) {
        if (rest.length < 2) {
            reply(invocation, "admin.check.usage");
            return;
        }
        String player = rest[1];
        mojangResolver.resolve(player).thenAccept(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                reply(invocation, "admin.check.player-not-found", "player", player);
                return;
            }
            UUID uuid = uuidOpt.get();
            long total = voteRepository.totalVotes(uuid);
            Optional<Instant> last = voteRepository.lastVoteAt(uuid);
            String lastText = last.map(LAST_VOTE_FORMAT::format).orElse("never");
            reply(invocation, "admin.check.result", "player", player, "total", String.valueOf(total), "last", lastText);
        });
    }

    private void handleParty(Invocation invocation, String[] rest) {
        if (rest.length < 2) {
            reply(invocation, "admin.party.usage");
            return;
        }
        int target = configManager.config().votePartyTarget();
        switch (rest[1].toLowerCase(Locale.ROOT)) {
            case "status" -> reply(invocation, "admin.party.status",
                    "current", String.valueOf(votePartyRepository.get()), "target", String.valueOf(target));
            case "reset" -> {
                votePartyRepository.reset();
                bridgeListener.pushProgress(0, target);
                reply(invocation, "admin.party.reset");
            }
            case "set" -> {
                if (rest.length < 3) {
                    reply(invocation, "admin.party.usage");
                    return;
                }
                Integer value = tryParseInt(rest[2]);
                if (value == null) {
                    reply(invocation, "admin.party.usage");
                    return;
                }
                votePartyRepository.set(value);
                bridgeListener.pushProgress(value, target);
                reply(invocation, "admin.party.set", "value", String.valueOf(value), "target", String.valueOf(target));
            }
            default -> reply(invocation, "admin.party.usage");
        }
    }

    private void replyCreditLikeResult(Invocation invocation, CreditResult result, String successKey, String notFoundKey,
                                        String player, String service) {
        if (!result.credited()) {
            reply(invocation, notFoundKey, "player", player);
            return;
        }
        reply(invocation, successKey, "player", player, "service", service);
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void reply(Invocation invocation, String key, Object... placeholders) {
        invocation.source().sendMessage(ColorText.of(configManager.messages().get(key, placeholders)));
    }
}
