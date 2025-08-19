package com.minekarta.kec.gui;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.util.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * The bank GUI for depositing and withdrawing emeralds.
 */
public class BankGui extends AbstractGui {

    private List<Integer> quickDepositAmounts;
    private List<Integer> quickWithdrawAmounts;

    public BankGui(KartaEmeraldCurrencyPlugin plugin, Player player) {
        super(plugin, player);
        this.quickDepositAmounts = plugin.getPluginConfig().getIntegerList("gui.quick-amounts");
        this.quickWithdrawAmounts = plugin.getPluginConfig().getIntegerList("gui.quick-amounts");
    }

    @Override
    public void open() {
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("bank-menu");
        if (guiConfig == null) {
            MessageUtil.sendRawMessage(player, "<red>Bank GUI not configured!</red>");
            return;
        }

        String title = guiConfig.getString("title", "Bank");
        int size = guiConfig.getInt("size", 54);
        createInventory(title, size);

        // Set static items
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig != null) {
            itemsConfig.getKeys(false).forEach(key -> {
                ConfigurationSection itemConfig = itemsConfig.getConfigurationSection(key);
                inventory.setItem(itemConfig.getInt("slot"), createItem(itemConfig));
            });
        }

        // Set quick deposit buttons
        populateQuickActionButtons(guiConfig, "quick-deposit", quickDepositAmounts);

        // Set quick withdraw buttons
        populateQuickActionButtons(guiConfig, "quick-withdraw", quickWithdrawAmounts);

        fill(guiConfig);
        player.openInventory(inventory);
    }

    private void populateQuickActionButtons(ConfigurationSection guiConfig, String type, List<Integer> amounts) {
        ConfigurationSection itemConfig = guiConfig.getConfigurationSection(type + "-item");
        List<Integer> slots = guiConfig.getIntegerList(type + "-slots");

        for (int i = 0; i < Math.min(slots.size(), amounts.size()); i++) {
            int amount = amounts.get(i);
            int slot = slots.get(i);

            TagResolver resolver = MessageUtil.placeholder("amount", plugin.getService().getFormatter().formatWithCommas(amount));
            ItemStack item = createItem(itemConfig, resolver);
            inventory.setItem(slot, item);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();
        ConfigurationSection guiConfig = plugin.getGuiConfig().getConfigurationSection("bank-menu");
        if (guiConfig == null) return;

        // Handle static items
        ConfigurationSection itemsConfig = guiConfig.getConfigurationSection("items");
        if (itemsConfig != null) {
            for (String key : itemsConfig.getKeys(false)) {
                if (itemsConfig.getInt(key + ".slot") == slot) {
                    handleStaticItemClick(key);
                    return;
                }
            }
        }

        // Handle quick deposit buttons
        if (handleQuickActionClick(guiConfig, "quick-deposit", quickDepositAmounts, slot, true)) return;

        // Handle quick withdraw buttons
        handleQuickActionClick(guiConfig, "quick-withdraw", quickWithdrawAmounts, slot, false);
    }

    private void handleStaticItemClick(String key) {
        switch (key) {
            case "deposit-custom":
                plugin.getChatInputManager().requestInput(player, TransactionType.DEPOSIT);
                break;
            case "withdraw-custom":
                plugin.getChatInputManager().requestInput(player, TransactionType.WITHDRAW);
                break;
            case "deposit-all":
                handleDepositAll();
                break;
            case "back-button":
                new MainGui(plugin, player).open();
                break;
            case "close-button":
                player.closeInventory();
                break;
        }
    }

    private boolean handleQuickActionClick(ConfigurationSection guiConfig, String type, List<Integer> amounts, int slot, boolean isDeposit) {
        List<Integer> slots = guiConfig.getIntegerList(type + "-slots");
        for (int i = 0; i < Math.min(slots.size(), amounts.size()); i++) {
            if (slots.get(i) == slot) {
                long amount = amounts.get(i);
                if (isDeposit) {
                    plugin.getService().depositToBank(player.getUniqueId(), amount).thenAccept(this::handleTransactionResult);
                } else {
                    plugin.getService().withdrawFromBank(player.getUniqueId(), amount).thenAccept(this::handleTransactionResult);
                }
                return true;
            }
        }
        return false;
    }

    private void handleDepositAll() {
        long walletBalance = plugin.getService().getWalletBalance(player);
        if (walletBalance <= 0) {
            MessageUtil.sendMessage(player, "deposit-fail-no-items");
            return;
        }
        plugin.getService().depositToBank(player.getUniqueId(), walletBalance).thenAccept(this::handleTransactionResult);
    }

    private void handleTransactionResult(boolean success) {
        if (success) {
            // Re-open the GUI on the main thread to show updated balance
            plugin.getServer().getScheduler().runTask(plugin, this::open);
        }
        // Messages for success/failure are sent from the service layer
    }
}
