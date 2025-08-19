package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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

public abstract class AbstractGui implements InventoryHolder {

    protected final KartaEmeraldCurrencyPlugin plugin;
    protected final Player player;
    protected Inventory inventory;

    public AbstractGui(KartaEmeraldCurrencyPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public abstract void open();

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
        if (itemConfig == null) return null;

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = itemConfig.getString("name", "");
            if (!name.isEmpty()) {
                meta.displayName(MiniMessage.miniMessage().deserialize(name));
            }

            List<String> loreLines = itemConfig.getStringList("lore");
            if (!loreLines.isEmpty()) {
                List<Component> lore = loreLines.stream()
                        .map(line -> MiniMessage.miniMessage().deserialize(line))
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
