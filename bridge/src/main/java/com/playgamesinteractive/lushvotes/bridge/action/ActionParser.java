package com.playgamesinteractive.lushvotes.bridge.action;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses "[TYPE] argument" strings (case-insensitive tag, e.g. both
 * "[MESSAGE]" and "[message]" work) into Actions. Lines that don't match the
 * bracket-tag format, or name an unknown type, are logged and skipped
 * rather than failing the whole menu. Same convention as lushlobby's
 * ActionParser.
 */
public final class ActionParser {

    private static final Pattern ACTION_PATTERN = Pattern.compile("^\\[(\\w+)]\\s?(.*)$");

    private ActionParser() {
    }

    public static List<Action> parse(List<String> raw, String context, Logger logger) {
        List<Action> actions = new ArrayList<>();
        for (String line : raw) {
            Matcher matcher = ACTION_PATTERN.matcher(line);
            if (!matcher.matches()) {
                logger.warning("Invalid action '" + line + "' in " + context + " - expected '[TYPE] argument'.");
                continue;
            }
            ActionType type;
            try {
                type = ActionType.valueOf(matcher.group(1).toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown action type '" + matcher.group(1) + "' in " + context);
                continue;
            }
            actions.add(new Action(type, matcher.group(2).trim()));
        }
        return actions;
    }
}
