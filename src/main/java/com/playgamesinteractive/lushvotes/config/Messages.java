package com.playgamesinteractive.lushvotes.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flattened dotted-key view of {@code language/en_US.yml} (proxy strings
 * only - see that file's header comment for why bridge has its own copy).
 */
public final class Messages {

    private final Map<String, String> flat;

    private Messages(Map<String, String> flat) {
        this.flat = flat;
    }

    public static Messages defaults() {
        return of(Map.of());
    }

    @SuppressWarnings("unchecked")
    public static Messages of(Map<String, Object> root) {
        Map<String, String> flat = new LinkedHashMap<>();
        flatten("", root, flat);
        return new Messages(flat);
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> node, Map<String, String> out) {
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> child) {
                flatten(key, (Map<String, Object>) child, out);
            } else if (entry.getValue() != null) {
                out.put(key, String.valueOf(entry.getValue()));
            }
        }
    }

    /** Raw string with {name} placeholders substituted; falls back to the key itself if missing. */
    public String get(String key, Object... placeholders) {
        String value = flat.get(key);
        if (value == null) {
            return key;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            value = value.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return value;
    }
}
