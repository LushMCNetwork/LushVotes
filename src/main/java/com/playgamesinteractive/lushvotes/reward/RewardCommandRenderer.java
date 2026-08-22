package com.playgamesinteractive.lushvotes.reward;

import java.util.ArrayList;
import java.util.List;

/**
 * Substitutes %player% into configured reward command templates. Rendering
 * happens here, on the proxy, so LushVotesBridge never needs to know the
 * template syntax or config shape - it just runs whatever strings it's
 * handed, staying a pure executor per the design. No %amount% - what a
 * reward actually is (currency, crate keys, items) is entirely up to the
 * literal command string an admin writes; there's no separate configurable
 * numeric amount feeding it.
 */
public final class RewardCommandRenderer {

    private RewardCommandRenderer() {
    }

    public static List<String> render(List<String> templates, String playerName) {
        List<String> rendered = new ArrayList<>(templates.size());
        for (String template : templates) {
            rendered.add(template.replace("%player%", playerName));
        }
        return rendered;
    }
}
