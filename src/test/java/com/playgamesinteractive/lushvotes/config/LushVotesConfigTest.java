package com.playgamesinteractive.lushvotes.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LushVotesConfigTest {

    @Test
    void defaultsMatchTheBundledConfigYml() {
        LushVotesConfig config = LushVotesConfig.defaults();
        assertTrue(config.enabled());
        assertEquals(20, config.dedupeWindowHours());
        assertEquals(List.of("eco give %player% 100"), config.rewardCommands());
        assertEquals(List.of("firework", "sound"), config.celebrationEffects());
        assertEquals("BALL_LARGE", config.fireworkType());
        assertEquals("#fdf700", config.fireworkColor());
        assertEquals("ENTITY_PLAYER_LEVELUP", config.celebrationSound());
        assertEquals(50, config.votePartyTarget());
        assertEquals(List.of("eco give %player% 500"), config.votePartyCommands());
    }

    @Test
    void fromMapParsesOverridesOverDefaults() {
        Map<String, Object> root = Map.of(
                "enabled", false,
                "dedupe", Map.of("window-hours", 5),
                "reward", Map.of("commands", List.of("gems give %player% 250")),
                "celebration", Map.of(
                        "effects", List.of("sound"),
                        "firework", Map.of("type", "STAR", "color", "RED"),
                        "sound", "ENTITY_FIREWORK_ROCKET_BLAST"),
                "vote-party", Map.of("target", 10, "commands", List.of("eco give %player% 1000"))
        );

        LushVotesConfig config = LushVotesConfig.fromMap(root);

        assertFalse(config.enabled());
        assertEquals(5, config.dedupeWindowHours());
        assertEquals(List.of("gems give %player% 250"), config.rewardCommands());
        assertEquals(List.of("sound"), config.celebrationEffects());
        assertEquals("STAR", config.fireworkType());
        assertEquals("RED", config.fireworkColor());
        assertEquals("ENTITY_FIREWORK_ROCKET_BLAST", config.celebrationSound());
        assertEquals(10, config.votePartyTarget());
        assertEquals(List.of("eco give %player% 1000"), config.votePartyCommands());
    }

    @Test
    void votePartyTargetIsClampedToAtLeastOne() {
        LushVotesConfig config = LushVotesConfig.fromMap(Map.of("vote-party", Map.of("target", 0)));
        assertEquals(1, config.votePartyTarget());
    }

    @Test
    void nullRootFallsBackToDefaults() {
        assertEquals(LushVotesConfig.defaults().rewardCommands(), LushVotesConfig.fromMap(null).rewardCommands());
    }
}
