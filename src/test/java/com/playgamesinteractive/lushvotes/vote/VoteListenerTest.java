package com.playgamesinteractive.lushvotes.vote;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoteListenerTest {

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals("Steve", VoteListener.normalizeUsername("  Steve  "));
    }

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", VoteListener.normalizeUsername(null));
    }

    @Test
    void alreadyCleanUsernameIsUnchanged() {
        assertEquals("Steve", VoteListener.normalizeUsername("Steve"));
    }
}
