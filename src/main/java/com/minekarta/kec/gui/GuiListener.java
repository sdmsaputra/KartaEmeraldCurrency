package com.minekarta.kec.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof AbstractGui gui) {
            // Prevent taking items from the GUI
            event.setCancelled(true);

            // Ensure the click is within the GUI inventory
            if (event.getClickedInventory() == gui.getInventory()) {
                gui.handleClick(event);
            }
        }
    }
}
