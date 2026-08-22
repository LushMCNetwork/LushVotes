package com.playgamesinteractive.lushvotes.bridge.menu;

import com.playgamesinteractive.lushvotes.bridge.action.ActionRunner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles clicks inside any inventory backed by a MenuHolder. Each item's
 * actions run through the shared ActionRunner, then the menu closes - these
 * are one-shot selector menus. Same convention as lushlobby's MenuListener.
 */
public class MenuListener implements Listener {

    private final ActionRunner actionRunner;

    public MenuListener(ActionRunner actionRunner) {
        this.actionRunner = actionRunner;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        MenuItem item = holder.menu().items().get(event.getSlot());
        if (item == null || item.actions().isEmpty()) {
            return;
        }
        if (item.permission() != null && !player.hasPermission(item.permission())) {
            return;
        }

        player.closeInventory();
        actionRunner.run(item.actions(), player);
    }
}
