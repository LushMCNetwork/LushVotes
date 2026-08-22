package com.playgamesinteractive.lushvotes.mojang;

import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real Mojang username->UUID lookup. Mojang returns the id as a 32-char
 * hex string with no dashes ({@code {"id":"...","name":"..."}}) - a single
 * field is not worth pulling in a JSON dependency for, so this pulls it
 * with a regex and reformats it into a dashed UUID by hand.
 */
public final class HttpMojangLookup implements MojangLookup {

    private static final Pattern ID_FIELD = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");

    private final HttpClient client;
    private final Logger logger;

    public HttpMojangLookup(Logger logger) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookup(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parse)
                .exceptionally(error -> {
                    logger.warn("Mojang lookup failed for '{}': {}", username, error.getMessage());
                    return Optional.empty();
                });
    }

    private Optional<UUID> parse(HttpResponse<String> response) {
        if (response.statusCode() == 204 || response.statusCode() == 404) {
            return Optional.empty(); // no such username
        }
        if (response.statusCode() != 200) {
            logger.warn("Mojang lookup returned HTTP {} for a username", response.statusCode());
            return Optional.empty();
        }
        Matcher matcher = ID_FIELD.matcher(response.body());
        if (!matcher.find()) {
            logger.warn("Mojang lookup response had no 'id' field: {}", response.body());
            return Optional.empty();
        }
        return Optional.of(dashed(matcher.group(1)));
    }

    private static UUID dashed(String hex32) {
        String dashedForm = hex32.substring(0, 8) + "-" + hex32.substring(8, 12) + "-" + hex32.substring(12, 16)
                + "-" + hex32.substring(16, 20) + "-" + hex32.substring(20, 32);
        return UUID.fromString(dashedForm);
    }
}
