package com.playgamesinteractive.lushvotes.reward;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardCommandRendererTest {

    @Test
    void substitutesPlayer() {
        List<String> result = RewardCommandRenderer.render(List.of("eco give %player% 100"), "Steve");
        assertEquals(List.of("eco give Steve 100"), result);
    }

    @Test
    void rendersEveryTemplateInTheList() {
        List<String> result = RewardCommandRenderer.render(
                List.of("eco give %player% 50", "broadcast %player% voted"), "Alex");
        assertEquals(List.of("eco give Alex 50", "broadcast Alex voted"), result);
    }

    @Test
    void aTemplateWithNoPlaceholderIsLeftAsIs() {
        List<String> result = RewardCommandRenderer.render(List.of("crates key giveall vote 1 -sf"), "Steve");
        assertEquals(List.of("crates key giveall vote 1 -sf"), result);
    }
}
