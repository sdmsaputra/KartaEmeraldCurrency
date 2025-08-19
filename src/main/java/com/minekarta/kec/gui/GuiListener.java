package com.minekarta.kec.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for GUI-related events.
 */
public class GuiListener implements Listener {

    /**
     * Constructs a new GuiListener.
     */
    public GuiListener() {
        // Default constructor
    }

    /**
     * Handles clicks within GUI inventories.
     * @param event The inventory click event.
     */
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
