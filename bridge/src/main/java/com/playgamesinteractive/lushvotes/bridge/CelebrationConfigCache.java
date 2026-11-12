package com.playgamesinteractive.lushvotes.bridge;

import java.util.List;

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
