package com.playgamesinteractive.lushvotes.bridge.util;

import com.playgamesinteractive.lushvotes.bridge.lang.ColorText;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ItemCosmetics {

    private ItemCosmetics() {
    }

    public static void decorate(ItemMeta meta, String displayName, List<String> lore,
                                 List<ItemFlag> itemFlags, Boolean enchantmentGlintOverride, boolean hideTooltip) {
        if (displayName != null && !displayName.isEmpty()) {
            meta.displayName(ColorText.of(displayName));
        }
        meta.lore(lore.stream().map(ColorText::of).toList());
        apply(meta, itemFlags, enchantmentGlintOverride);
        meta.setHideTooltip(hideTooltip);
    }

    public static List<ItemFlag> parseItemFlags(List<String> raw, String context, Logger logger) {
        List<ItemFlag> flags = new ArrayList<>();
        for (String name : raw) {
            try {
                flags.add(ItemFlag.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warning("Unknown item flag '" + name + "' in " + context);
            }
        }
        return flags;
    }

    public static void apply(ItemMeta meta, List<ItemFlag> itemFlags, Boolean enchantmentGlintOverride) {
        if (!itemFlags.isEmpty()) {
            meta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));
        }
        if (enchantmentGlintOverride != null) {
            if (enchantmentGlintOverride) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeEnchant(Enchantment.UNBREAKING);
            }
        }
    }
}
