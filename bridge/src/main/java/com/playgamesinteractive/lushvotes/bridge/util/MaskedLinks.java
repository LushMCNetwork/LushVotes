package com.playgamesinteractive.lushvotes.bridge.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites {@code [url](text)} - a shorthand carried over from how this
 * network's menus were authored in DeluxeMenus - into MiniMessage's own
 * {@code <click:open_url:'url'>text</click>} tag, so {@code text} is all the
 * client ever shows while the real URL only fires on click. Only
 * {@link com.playgamesinteractive.lushvotes.bridge.lang.ColorText#ofWithLinks}
 * enables the click tag, since general config/lore text stays restricted to
 * color/decoration tags - see that class's doc.
 */
public final class MaskedLinks {

    private static final Pattern LINK = Pattern.compile("\\[(https?://[^]\\s]+)]\\(([^)]*)\\)");

    private MaskedLinks() {
    }

    public static String preprocess(String raw) {
        Matcher matcher = LINK.matcher(raw);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String url = matcher.group(1);
            String text = matcher.group(2);
            matcher.appendReplacement(result, Matcher.quoteReplacement(
                    "<click:open_url:'" + url + "'>" + text + "</click>"));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
