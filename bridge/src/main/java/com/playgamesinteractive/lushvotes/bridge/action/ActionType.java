package com.playgamesinteractive.lushvotes.bridge.action;

/**
 * The fixed vocabulary of bracket-tag actions usable in menus/*.yml. Same
 * set and convention as lushlobby's ActionType (MENU/PROXYCOMMAND aren't
 * ported here - voting never needs to open a second menu or transfer the
 * player to another server). A masked link isn't its own action type -
 * write {@code [url](text)} directly inside a MESSAGE line; see MaskedLinks.
 */
public enum ActionType {
    CONSOLE,
    PLAYER,
    MESSAGE
}
