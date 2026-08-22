package com.playgamesinteractive.lushvotes.bridge;

import java.util.List;

/**
 * Local mirror of the celebration section of LushVotes' config.yml, pushed
 * down over {@link VotesChannel} - see {@code VotesBridgeListener} on the
 * proxy side. Not loaded until the first sync arrives; {@link #isLoaded()}
 * lets {@code ConfigSyncRequester}-style startup catch-up know when to stop
 * asking, same pattern as LushRelay's config caches. Broadcast was removed
 * (see CelebrationEffects' doc), and the unclaimed-rewards join reminder
 * always shows rather than being config-gated - what's left is which
 * effects are enabled, plus the firework/sound specifics for them.
 */
final class CelebrationConfigCache {

    private volatile boolean loaded = false;
    private volatile List<String> effects = List.of();
    private volatile String fireworkType = "BALL_LARGE";
    private volatile String fireworkColor = "YELLOW";
    private volatile String sound = "ENTITY_PLAYER_LEVELUP";

    void update(List<String> effects, String fireworkType, String fireworkColor, String sound) {
        this.effects = List.copyOf(effects);
        this.fireworkType = fireworkType;
        this.fireworkColor = fireworkColor;
        this.sound = sound;
        this.loaded = true;
    }

    boolean isLoaded() {
        return loaded;
    }

    List<String> effects() {
        return effects;
    }

    String fireworkType() {
        return fireworkType;
    }

    String fireworkColor() {
        return fireworkColor;
    }

    String sound() {
        return sound;
    }
}
