package com.playgamesinteractive.lushvotes.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LushVotesCommandTest {

    @Test
    void keepsOnlyOptionsStartingWithThePrefix() {
        List<String> result = LushVotesCommand.filter(List.of("reload", "credit", "testvote", "check", "party"), "c");
        assertEquals(List.of("credit", "check"), result);
    }

    @Test
    void emptyPrefixReturnsEverything() {
        List<String> options = List.of("status", "reset", "set");
        assertEquals(options, LushVotesCommand.filter(options, ""));
    }

    @Test
    void isCaseInsensitive() {
        List<String> result = LushVotesCommand.filter(List.of("Steve", "Alex"), "st");
        assertEquals(List.of("Steve"), result);
    }

    @Test
    void noMatchesReturnsAnEmptyList() {
        assertEquals(List.of(), LushVotesCommand.filter(List.of("reload", "credit"), "z"));
    }
}
