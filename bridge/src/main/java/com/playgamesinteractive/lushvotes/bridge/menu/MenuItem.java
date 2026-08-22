package com.playgamesinteractive.lushvotes.bridge.menu;

import com.playgamesinteractive.lushvotes.bridge.action.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

/**
 * A single configured slot in a menus/*.yml file. Kept as raw config values
 * rather than a pre-built {@code ItemStack} - display name and lore are
 * resolved through PlaceholderAPI per viewer at {@link MenuManager#open}
 * time (e.g. %lushvotes_total_formatted%), so the same menu can't share one
 * cached icon across different players.
 * <p>
 * {@code hideTooltip} is its own field, not folded into {@code itemFlags} -
 * full-tooltip hiding is {@code ItemMeta#setHideTooltip(boolean)} on this
 * API, a real per-item property, not an {@code ItemFlag} constant (the only
 * tooltip-related flag that actually exists is {@code HIDE_ADDITIONAL_TOOLTIP},
 * which only hides the enchant/attribute lines, not the whole tooltip).
 */
public record MenuItem(int slot, Material material, String displayName, List<String> lore,
                        List<ItemFlag> itemFlags, Boolean enchantmentGlintOverride, boolean hideTooltip,
                        List<Action> actions, String permission) {
}
