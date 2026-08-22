package com.playgamesinteractive.lushvotes.bridge.util;

import com.playgamesinteractive.lushvotes.bridge.lang.ColorText;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Shared by every menu item icon builder. {@code enchantment_glint_override}
 * adds a real (but harmless) enchantment so the item genuinely glows, then
 * hides the enchantment line from the tooltip - a plain
 * {@code setEnchantmentGlintOverride()} call only fakes the glint and
 * doesn't survive every client/resource-pack combination the way a real
 * enchant + HIDE_ENCHANTS does. Leave {@code enchantmentGlintOverride} null
 * to not touch glint at all. Same convention as lushlobby's ItemCosmetics.
 * {@code hideTooltip} is its own boolean argument, not an item flag - see
 * MenuItem's doc for why {@code HIDE_TOOLTIP} isn't a real flag here.
 */
public final class ItemCosmetics {

    private ItemCosmetics() {
    }

    /** Colorized display name (skipped when empty), colorized lore, then flags/glint/tooltip - the shared decoration sequence for every configured item. */
    public static void decorate(ItemMeta meta, String displayName, List<String> lore,
                                 List<ItemFlag> itemFlags, Boolean enchantmentGlintOverride, boolean hideTooltip) {
        if (displayName != null && !displayName.isEmpty()) {
            meta.displayName(ColorText.of(displayName));
        }
        meta.lore(lore.stream().map(ColorText::of).toList());
        apply(meta, itemFlags, enchantmentGlintOverride);
        meta.setHideTooltip(hideTooltip);
    }

    /** Shared "item_flags:" list parsing for menus/*.yml - unknown names are logged and skipped. */
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
