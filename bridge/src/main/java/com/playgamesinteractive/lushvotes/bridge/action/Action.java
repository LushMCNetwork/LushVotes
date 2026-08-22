package com.playgamesinteractive.lushvotes.bridge.action;

/**
 * One parsed "[TYPE] argument" line, e.g. "[MESSAGE] <green>Thanks!" or
 * "[URL] https://example.com|<yellow>Click here". argument() may be empty
 * but never null.
 */
public record Action(ActionType type, String argument) {
}
