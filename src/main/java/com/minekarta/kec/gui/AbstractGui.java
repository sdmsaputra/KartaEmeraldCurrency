package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
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

    protected void createInventory(String title, int size, TagResolver... resolvers) {
        String processedTitle = PlaceholderAPI.setPlaceholders(player, title);
        Component parsedTitle = MiniMessage.miniMessage().deserialize(processedTitle, resolvers);
        this.inventory = Bukkit.createInventory(this, size, parsedTitle);
    }

    protected ItemStack createItem(ConfigurationSection itemConfig, TagResolver... resolvers) {
        if (itemConfig == null) return null;

        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = itemConfig.getString("name", "");
            if (!name.isEmpty()) {
                String processedName = PlaceholderAPI.setPlaceholders(player, "<italic:false>" + name);
                meta.displayName(MiniMessage.miniMessage().deserialize(processedName, resolvers));
            }

            List<String> loreLines = itemConfig.getStringList("lore");
            if (!loreLines.isEmpty()) {
                List<Component> lore = loreLines.stream()
                        .map(line -> {
                            String processedLine = PlaceholderAPI.setPlaceholders(player, "<italic:false>" + line);
                            return MiniMessage.miniMessage().deserialize(processedLine, resolvers);
                        })
                        .collect(Collectors.toList());
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    protected void fill(ConfigurationSection guiConfig, TagResolver... resolvers) {
        ConfigurationSection fillConfig = guiConfig.getConfigurationSection("fill-item");
        if (fillConfig == null) return;

        ItemStack fillItem = createItem(fillConfig, resolvers);
        if (fillItem == null) return;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, fillItem);
            }
        }
    }
}
