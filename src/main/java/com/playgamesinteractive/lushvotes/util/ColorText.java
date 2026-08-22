package com.playgamesinteractive.lushvotes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

/**
 * Shared text rendering for anything sourced from config/language files -
 * MiniMessage tags only (e.g. {@code <red>}, {@code <#rrggbb>}, {@code <bold>}),
 * same restricted tag set and rationale as LushAuctions/LushShop's ColorText:
 * click/hover/insertion/font/translatable/selector/nbt/score tags are
 * deliberately left out since config values aren't fully trusted input.
 */
public final class ColorText {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.transition(),
                    StandardTags.reset(),
                    StandardTags.newline()))
            .build();

    private ColorText() {
    }

    public static Component of(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(raw).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
