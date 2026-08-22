package com.playgamesinteractive.lushvotes.bridge.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskedLinksTest {

    @Test
    void rewritesAMaskedLinkIntoAMiniMessageClickTag() {
        String result = MaskedLinks.preprocess("Vote here: [https://example.com](Click here!)");
        assertEquals("Vote here: <click:open_url:'https://example.com'>Click here!</click>", result);
    }

    @Test
    void leavesPlainTextWithNoLinkUnchanged() {
        assertEquals("<green>No links here</green>", MaskedLinks.preprocess("<green>No links here</green>"));
    }

    @Test
    void rewritesMultipleLinksInTheSameString() {
        String result = MaskedLinks.preprocess("[https://a.com](A) and [https://b.com](B)");
        assertEquals("<click:open_url:'https://a.com'>A</click> and <click:open_url:'https://b.com'>B</click>", result);
    }

    @Test
    void emptyDisplayTextIsPreserved() {
        String result = MaskedLinks.preprocess("[https://example.com]()");
        assertEquals("<click:open_url:'https://example.com'></click>", result);
    }
}
