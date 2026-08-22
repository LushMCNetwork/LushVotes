package com.playgamesinteractive.lushvotes.bridge.util;

import org.bukkit.Color;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FireworkColorsTest {

    private static final Logger LOGGER = Logger.getLogger(FireworkColorsTest.class.getName());

    @Test
    void resolvesAKnownColorByName() {
        assertEquals(Color.RED, FireworkColors.parse("RED", Color.WHITE, LOGGER));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals(Color.YELLOW, FireworkColors.parse("yellow", Color.WHITE, LOGGER));
    }

    @Test
    void fallsBackToTheDefaultForAnUnknownName() {
        assertEquals(Color.WHITE, FireworkColors.parse("NOT_A_REAL_COLOR", Color.WHITE, LOGGER));
    }

    @Test
    void resolvesAHexColor() {
        assertEquals(Color.fromRGB(0xFF00AA), FireworkColors.parse("#FF00AA", Color.WHITE, LOGGER));
    }

    @Test
    void hexIsCaseInsensitive() {
        assertEquals(Color.fromRGB(0xff00aa), FireworkColors.parse("#ff00aa", Color.WHITE, LOGGER));
    }

    @Test
    void fallsBackForAMalformedHexColor() {
        assertEquals(Color.WHITE, FireworkColors.parse("#zzzzzz", Color.WHITE, LOGGER));
        assertEquals(Color.WHITE, FireworkColors.parse("#fff", Color.WHITE, LOGGER));
    }
}
