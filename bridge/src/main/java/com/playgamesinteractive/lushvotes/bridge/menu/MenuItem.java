package com.playgamesinteractive.lushvotes.bridge.menu;

import com.playgamesinteractive.lushvotes.bridge.action.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.List;

public record MenuItem(int slot, Material material, String displayName, List<String> lore,
                        List<ItemFlag> itemFlags, Boolean enchantmentGlintOverride, boolean hideTooltip,
                        List<Action> actions, String permission) {
}
