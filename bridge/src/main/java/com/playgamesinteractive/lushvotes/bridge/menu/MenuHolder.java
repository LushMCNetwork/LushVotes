package com.playgamesinteractive.lushvotes.bridge.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {

    private final Menu menu;
    private Inventory inventory;

    public MenuHolder(Menu menu) {
        this.menu = menu;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Menu menu() {
        return menu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
