package com.playgamesinteractive.lushvotes.bridge.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
