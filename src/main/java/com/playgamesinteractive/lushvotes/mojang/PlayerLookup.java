package com.playgamesinteractive.lushvotes.mojang;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Resolves a voter's name for either edition. Floodgate gives Bedrock players a prefixed name
 * ({@code _Steve}) and a UUID built from their XUID, so a prefixed name must never go to Mojang
 * first: a Java account may own the same spelling and would receive the reward.
 *
 * <p>Order: a player online on the proxy, then players the network has seen (which covers Java
 * players whose names start with the prefix), then Bedrock (Xbox) for prefixed names or Mojang
 * for the rest. An unprefixed name Mojang doesn't know is tried as a gamertag; a prefixed name
 * Xbox doesn't know is left unresolved rather than guessed.
 */
public final class PlayerLookup implements MojangLookup {

    private final Function<String, Optional<UUID>> online;
    private final Function<String, Optional<UUID>> known;
    private final MojangLookup java;
    private final MojangLookup bedrock;
    private final String prefix;

    public PlayerLookup(Function<String, Optional<UUID>> online, Function<String, Optional<UUID>> known,
                        MojangLookup java, MojangLookup bedrock, String prefix) {
        this.online = online;
        this.known = known;
        this.java = java;
        this.bedrock = bedrock;
        this.prefix = prefix == null ? "" : prefix;
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookup(String username) {
        Optional<UUID> found = online.apply(username);
        if (found.isPresent()) return CompletableFuture.completedFuture(found);
        boolean prefixed = !prefix.isEmpty() && username.startsWith(prefix) && username.length() > prefix.length();
        String gamertag = prefixed ? username.substring(prefix.length()) : username;
        return CompletableFuture.supplyAsync(() -> known.apply(username)
                        .or(() -> prefixed ? Optional.empty() : known.apply(prefix + username)))
                .thenCompose(seen -> {
                    if (seen.isPresent()) return CompletableFuture.completedFuture(seen);
                    if (prefixed) return bedrock.lookup(gamertag);
                    return orElse(java.lookup(username), () -> bedrock.lookup(username));
                });
    }

    private static CompletableFuture<Optional<UUID>> orElse(CompletableFuture<Optional<UUID>> first,
                                                            Supplier<CompletableFuture<Optional<UUID>>> next) {
        return first.thenCompose(result -> result.isPresent() ? CompletableFuture.completedFuture(result) : next.get());
    }
}
