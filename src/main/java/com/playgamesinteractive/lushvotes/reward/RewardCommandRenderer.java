package com.playgamesinteractive.lushvotes.reward;

import java.util.ArrayList;
import java.util.List;

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
