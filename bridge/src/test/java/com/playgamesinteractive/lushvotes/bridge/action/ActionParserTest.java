package com.playgamesinteractive.lushvotes.bridge.action;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionParserTest {

    private static final Logger LOGGER = Logger.getLogger(ActionParserTest.class.getName());

    @Test
    void parsesAMessageActionKeepingAMaskedLinkIntact() {
        List<Action> actions = ActionParser.parse(
                List.of("[MESSAGE] Vote here: [https://example.com](<yellow>Click here!)"), "test", LOGGER);

        assertEquals(1, actions.size());
        assertEquals(ActionType.MESSAGE, actions.get(0).type());
        assertEquals("Vote here: [https://example.com](<yellow>Click here!)", actions.get(0).argument());
    }

    @Test
    void tagIsCaseInsensitive() {
        List<Action> actions = ActionParser.parse(List.of("[message] <green>hi"), "test", LOGGER);
        assertEquals(ActionType.MESSAGE, actions.get(0).type());
    }

    @Test
    void malformedLineIsSkippedNotThrown() {
        List<Action> actions = ActionParser.parse(List.of("not an action line"), "test", LOGGER);
        assertTrue(actions.isEmpty());
    }

    @Test
    void unknownTypeIsSkippedNotThrown() {
        List<Action> actions = ActionParser.parse(List.of("[TELEPORT] spawn"), "test", LOGGER);
        assertTrue(actions.isEmpty());
    }

    @Test
    void anEmptyMessageLineParsesToAnEmptyArgument() {
        List<Action> actions = ActionParser.parse(List.of("[message] "), "test", LOGGER);
        assertEquals("", actions.get(0).argument());
    }

    @Test
    void multipleValidLinesAllParse() {
        List<Action> actions = ActionParser.parse(
                List.of("[MESSAGE] hi", "[CONSOLE] say hi"), "test", LOGGER);
        assertEquals(2, actions.size());
    }
}
