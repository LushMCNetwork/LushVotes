package com.playgamesinteractive.lushvotes.bridge.menu;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public record Menu(String id, String title, int size, ItemStack fill, ItemStack corner,
                    ItemStack accent, List<Integer> accentSlots, Map<Integer, MenuItem> items) {

    public List<MenuItem> itemList() {
        return List.copyOf(items().values());
    }
}
