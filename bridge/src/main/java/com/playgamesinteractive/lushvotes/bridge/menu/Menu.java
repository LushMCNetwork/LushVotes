package com.playgamesinteractive.lushvotes.bridge.menu;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Three optional background layers, painted in this order before any real
 * item goes down on top:
 * <ol>
 *   <li>{@code fill} - every slot in the inventory, so it ends up being
 *   "whatever's left over" once the other layers and real items are
 *   painted on top.</li>
 *   <li>{@code corner} - always exactly two slots: 0 and {@code size - 1}.
 *   Not configurable as a slot list - it's always just those two.</li>
 *   <li>{@code accent} - painted at whichever slots {@code accentSlots}
 *   lists; the only layer you actually pick slots for.</li>
 * </ol>
 * Any layer left {@code null} is skipped entirely. Real {@link MenuItem}s
 * are placed after all three, so a content slot always wins.
 */
public record Menu(String id, String title, int size, ItemStack fill, ItemStack corner,
                    ItemStack accent, List<Integer> accentSlots, Map<Integer, MenuItem> items) {

    public List<MenuItem> itemList() {
        return List.copyOf(items().values());
    }
}
