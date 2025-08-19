package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an abstract GUI screen.
 */
public abstract class AbstractGui implements InventoryHolder {

    /**
     * The plugin instance.
     */
    protected final KartaEmeraldCurrencyPlugin plugin;
    /**
     * The player viewing the GUI.
     */
    protected final Player player;
    /**
     * The inventory for this GUI.
     */
    protected Inventory inventory;

    /**
     * Constructs a new AbstractGui.
     * @param plugin The plugin instance.
     * @param player The player viewing the GUI.
     */
    public AbstractGui(KartaEmeraldCurrencyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /**
     * Opens the GUI for the player.
     */
    public abstract void open();

    /**
     * Handles a click event in the GUI.
     * @param event The click event.
     */
    public abstract void handleClick(InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    protected void createInventory(String title, int size) {
        Component parsedTitle = MiniMessage.miniMessage().deserialize(title);
        this.inventory = Bukkit.createInventory(this, size, parsedTitle);
    }

    protected ItemStack createItem(ConfigurationSection itemConfig) {
        return createItem(itemConfig, TagResolver.empty());
    }

    protected ItemStack createItem(ConfigurationSection itemConfig, TagResolver resolvers) {
        if (itemConfig == null) return null;

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = itemConfig.getString("name", "");
            if (!name.isEmpty()) {
                // To prevent italics, we must deserialize with a tag resolver that includes the player's name.
                // However, since we don't have the player here, we will just have to do it in the GUI class.
                meta.displayName(MiniMessage.miniMessage().deserialize(name, resolvers));
            }

            List<String> loreLines = itemConfig.getStringList("lore");
            if (!loreLines.isEmpty()) {
                List<Component> lore = loreLines.stream()
                        .map(line -> MiniMessage.miniMessage().deserialize("<italic:false>" + line, resolvers))
                        .collect(Collectors.toList());
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    protected void fill(ConfigurationSection guiConfig) {
        ConfigurationSection fillConfig = guiConfig.getConfigurationSection("fill-item");
        if (fillConfig == null) return;

        ItemStack fillItem = createItem(fillConfig);
        if (fillItem == null) return;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }
}
