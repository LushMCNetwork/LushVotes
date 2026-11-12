package com.playgamesinteractive.lushvotes.mojang;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MojangLookup {

    CompletableFuture<Optional<UUID>> lookup(String username);
}
