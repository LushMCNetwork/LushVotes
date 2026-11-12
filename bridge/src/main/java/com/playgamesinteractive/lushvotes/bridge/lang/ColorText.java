package com.playgamesinteractive.lushvotes.bridge.lang;

import com.playgamesinteractive.lushvotes.bridge.util.MaskedLinks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

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

    private static final MiniMessage MINI_MESSAGE_WITH_LINKS = MiniMessage.builder()
            .tags(TagResolver.resolver(
                    StandardTags.color(),
                    StandardTags.decorations(),
                    StandardTags.gradient(),
                    StandardTags.rainbow(),
                    StandardTags.transition(),
                    StandardTags.reset(),
                    StandardTags.newline(),
                    StandardTags.clickEvent()))
            .build();

    private ColorText() {
    }

    public static Component of(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(raw).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static Component ofWithLinks(String raw) {
        if (raw == null) {
            return Component.empty();
        }
        return MINI_MESSAGE_WITH_LINKS.deserialize(MaskedLinks.preprocess(raw))
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
