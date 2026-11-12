package com.playgamesinteractive.lushvotes.mojang;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerLookupTest {

    private static final UUID JAVA = UUID.fromString("9696c571-bf88-4ccf-af0c-a94f87d2a1a2");
    private static final UUID BEDROCK = new UUID(0L, 2535428580449290L);
    private static final UUID ONLINE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final List<String> calls = new ArrayList<>();

    private MojangLookup fake(String edition, Map<String, UUID> answers) {
        return name -> {
            calls.add(edition + ":" + name);
            return CompletableFuture.completedFuture(Optional.ofNullable(answers.get(name)));
        };
    }

    private PlayerLookup lookup(Map<String, UUID> online, Map<String, UUID> known) {
        return new PlayerLookup(name -> Optional.ofNullable(online.get(name)), name -> Optional.ofNullable(known.get(name)),
                fake("java", Map.of("_Xenon60_", JAVA, "Steve", JAVA)), fake("bedrock", Map.of("Xenon60_", BEDROCK, "Alex", BEDROCK)), "_");
    }

    @Test
    void prefixedNamesNeverReachMojang() throws Exception {
        assertEquals(Optional.of(BEDROCK), lookup(Map.of(), Map.of()).lookup("_Xenon60_").get());
        assertEquals(List.of("bedrock:Xenon60_"), calls);
    }

    @Test
    void unknownPrefixedNameStaysUnresolved() throws Exception {
        assertEquals(Optional.empty(), lookup(Map.of(), Map.of()).lookup("_Nobody").get());
        assertEquals(List.of("bedrock:Nobody"), calls);
    }

    @Test
    void javaPlayerWithPrefixedNameIsFoundThroughTheNetwork() throws Exception {
        assertEquals(Optional.of(JAVA), lookup(Map.of(), Map.of("_Xenon60_", JAVA)).lookup("_Xenon60_").get());
        assertEquals(List.of(), calls);
    }

    @Test
    void onlinePlayersWinOverEverything() throws Exception {
        assertEquals(Optional.of(ONLINE), lookup(Map.of("Steve", ONLINE), Map.of()).lookup("Steve").get());
        assertEquals(List.of(), calls);
    }

    @Test
    void unprefixedNameTriesJavaThenBedrock() throws Exception {
        assertEquals(Optional.of(JAVA), lookup(Map.of(), Map.of()).lookup("Steve").get());
        assertEquals(Optional.of(BEDROCK), lookup(Map.of(), Map.of()).lookup("Alex").get());
        assertEquals(List.of("java:Steve", "java:Alex", "bedrock:Alex"), calls);
    }

    @Test
    void gamertagWithoutPrefixMatchesKnownBedrockPlayer() throws Exception {
        assertEquals(Optional.of(BEDROCK), lookup(Map.of(), Map.of("_Alex", BEDROCK)).lookup("Alex").get());
        assertEquals(List.of(), calls);
    }

    @Test
    void xuidBecomesFloodgateUuid() {
        assertEquals(Optional.of(BEDROCK), HttpBedrockLookup.parse(200, "{\"xuid\":2535428580449290}"));
        assertEquals(Optional.empty(), HttpBedrockLookup.parse(503, "{\"message\":\"Unable to find user\"}"));
    }
}
