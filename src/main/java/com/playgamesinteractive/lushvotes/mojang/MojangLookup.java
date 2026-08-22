package com.playgamesinteractive.lushvotes.mojang;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Seam so {@link MojangResolver} is testable without hitting the real Mojang API. */
public interface MojangLookup {

    CompletableFuture<Optional<UUID>> lookup(String username);
}
