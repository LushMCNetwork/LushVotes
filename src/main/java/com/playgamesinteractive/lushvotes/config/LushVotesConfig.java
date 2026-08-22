package com.playgamesinteractive.lushvotes.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Typed, immutable view of {@code config.yml}. Built by {@link ConfigManager}
 * from a raw SnakeYAML {@code Map<String, Object>} - see its class doc for
 * why Velocity plugins parse config by hand instead of Bukkit's
 * {@code ConfigurationSection} (which doesn't exist on this platform).
 * <p>
 * No site list here - {@code /vote} is a LushVotesBridge inventory GUI now
 * (see bridge's {@code menus/vote_menu.yml}), hand-authored per site rather
 * than templated from proxy config, since each site needs its own icon/lore
 * the way any other menu button does. No reward amount either - a reward is
 * whatever literal command an admin writes (currency, crate keys, items),
 * not a number fed through a fixed template.
 * <p>
 * {@code celebration.firework.type}/{@code .color} and {@code
 * celebration.sound} are carried as plain strings - Velocity has no Bukkit
 * on its classpath, so this side can't validate them against real
 * {@code FireworkEffect.Type}/{@code Color}/{@code Sound} values. LushVotesBridge
 * does that parsing (with a log+fallback for a bad name) since it's the
 * only side that actually has those enums - see its CelebrationEffects.
 */
public final class LushVotesConfig {

    private final boolean enabled;
    private final String language;
    private final int dedupeWindowHours;
    private final List<String> rewardCommands;
    private final List<String> celebrationEffects;
    private final String fireworkType;
    private final String fireworkColor;
    private final String celebrationSound;
    private final int votePartyTarget;
    private final List<String> votePartyCommands;

    private LushVotesConfig(boolean enabled, String language, int dedupeWindowHours,
                             List<String> rewardCommands, List<String> celebrationEffects,
                             String fireworkType, String fireworkColor, String celebrationSound,
                             int votePartyTarget, List<String> votePartyCommands) {
        this.enabled = enabled;
        this.language = language;
        this.dedupeWindowHours = dedupeWindowHours;
        this.rewardCommands = List.copyOf(rewardCommands);
        this.celebrationEffects = List.copyOf(celebrationEffects);
        this.fireworkType = fireworkType;
        this.fireworkColor = fireworkColor;
        this.celebrationSound = celebrationSound;
        this.votePartyTarget = votePartyTarget;
        this.votePartyCommands = List.copyOf(votePartyCommands);
    }

    public static LushVotesConfig defaults() {
        return new LushVotesConfig(true, "en_US", 20,
                List.of("eco give %player% 100"), List.of("firework", "sound"),
                "BALL_LARGE", "#fdf700", "ENTITY_PLAYER_LEVELUP",
                50, List.of("eco give %player% 500"));
    }

    @SuppressWarnings("unchecked")
    public static LushVotesConfig fromMap(Map<String, Object> root) {
        if (root == null) {
            return defaults();
        }
        LushVotesConfig d = defaults();

        boolean enabled = boolOr(root.get("enabled"), d.enabled);
        String language = stringOr(root.get("language"), d.language);

        Map<String, Object> dedupe = mapOr(root.get("dedupe"));
        int dedupeWindowHours = intOr(dedupe.get("window-hours"), d.dedupeWindowHours);

        Map<String, Object> reward = mapOr(root.get("reward"));
        List<String> rewardCommands = stringListOr(reward.get("commands"), d.rewardCommands);

        Map<String, Object> celebration = mapOr(root.get("celebration"));
        List<String> celebrationEffects = stringListOr(celebration.get("effects"), d.celebrationEffects);
        Map<String, Object> firework = mapOr(celebration.get("firework"));
        String fireworkType = stringOr(firework.get("type"), d.fireworkType);
        String fireworkColor = stringOr(firework.get("color"), d.fireworkColor);
        String celebrationSound = stringOr(celebration.get("sound"), d.celebrationSound);

        Map<String, Object> votePartyMap = mapOr(root.get("vote-party"));
        int votePartyTarget = Math.max(1, intOr(votePartyMap.get("target"), d.votePartyTarget));
        List<String> votePartyCommands = stringListOr(votePartyMap.get("commands"), d.votePartyCommands);

        return new LushVotesConfig(enabled, language, dedupeWindowHours, rewardCommands,
                celebrationEffects, fireworkType, fireworkColor, celebrationSound,
                votePartyTarget, votePartyCommands);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOr(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static boolean boolOr(Object value, boolean def) {
        return value instanceof Boolean b ? b : def;
    }

    private static String stringOr(Object value, String def) {
        return value != null ? String.valueOf(value) : def;
    }

    private static int intOr(Object value, int def) {
        return value instanceof Number n ? n.intValue() : def;
    }

    private static List<String> stringListOr(Object value, List<String> def) {
        if (!(value instanceof List<?> list)) {
            return def;
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    public boolean enabled() {
        return enabled;
    }

    public String language() {
        return language;
    }

    public int dedupeWindowHours() {
        return dedupeWindowHours;
    }

    public List<String> rewardCommands() {
        return rewardCommands;
    }

    public List<String> celebrationEffects() {
        return celebrationEffects;
    }

    /** {@code FireworkEffect.Type} name, e.g. "BALL_LARGE" - validated on the bridge, not here. */
    public String fireworkType() {
        return fireworkType;
    }

    /** {@code org.bukkit.Color} field name (e.g. "YELLOW") or a "#rrggbb" hex string - resolved on the bridge, not here. */
    public String fireworkColor() {
        return fireworkColor;
    }

    /** {@code Sound} enum name, e.g. "ENTITY_PLAYER_LEVELUP" - validated on the bridge, not here. */
    public String celebrationSound() {
        return celebrationSound;
    }

    /** Votes needed network-wide to trigger a vote party (rewards every online player). */
    public int votePartyTarget() {
        return votePartyTarget;
    }

    public List<String> votePartyCommands() {
        return votePartyCommands;
    }
}
