package com.playgamesinteractive.lushvotes.mojang;

import org.slf4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks a Bedrock gamertag up through GeyserMC's global API and returns the UUID Floodgate gives
 * that player: {@code new UUID(0, xuid)}. Floodgate replaces spaces with underscores, so a name
 * with underscores is retried with spaces.
 */
public final class HttpBedrockLookup implements MojangLookup {

    private static final Pattern XUID_FIELD = Pattern.compile("\"xuid\"\\s*:\\s*\"?(\\d+)");

    private final HttpClient client;
    private final Logger logger;

    public HttpBedrockLookup(Logger logger) {
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.logger = logger;
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookup(String gamertag) {
        CompletableFuture<Optional<UUID>> first = request(gamertag);
        if (!gamertag.contains("_")) return first;
        return first.thenCompose(found -> found.isPresent()
                ? CompletableFuture.completedFuture(found)
                : request(gamertag.replace('_', ' ')));
    }

    private CompletableFuture<Optional<UUID>> request(String gamertag) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.geysermc.org/v2/xbox/xuid/"
                        + URLEncoder.encode(gamertag, StandardCharsets.UTF_8).replace("+", "%20")))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(response.statusCode(), response.body()))
                .exceptionally(error -> {
                    logger.warn("Bedrock lookup failed for '{}': {}", gamertag, error.getMessage());
                    return Optional.empty();
                });
    }

    static Optional<UUID> parse(int status, String body) {
        if (status != 200 || body == null) return Optional.empty();
        Matcher matcher = XUID_FIELD.matcher(body);
        if (!matcher.find()) return Optional.empty();
        try {
            return Optional.of(new UUID(0L, Long.parseLong(matcher.group(1))));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
