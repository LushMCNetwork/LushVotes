package com.playgamesinteractive.lushvotes.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Full SimpleCommand.suggest() isn't exercised here - Velocity's Invocation/
 * ProxyServer are large interfaces this project doesn't otherwise mock (see
 * VoteCommandTest in the bridge module, which only tests its own extracted
 * pure function). filter() is the one general-purpose piece of the new
 * tab-completion logic, so it's what's covered.
 */
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
