package com.minekarta.kec.command;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.KartaEmeraldService;
import com.minekarta.kec.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EmeraldCommand implements CommandExecutor, TabCompleter {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final KartaEmeraldService service;

    public EmeraldCommand(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                MessageUtil.sendMessage(sender, "console-not-supported");
                return true;
            }
            if (!player.hasPermission("kec.gui")) {
                MessageUtil.sendMessage(player, "no-permission");
                return true;
            }
            new com.minekarta.kec.gui.MainGui(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "balance", "bal" -> handleBalance(sender, args);
            case "help" -> handleHelp(sender);
            case "pay" -> handlePay(sender, args);
            case "deposit" -> handleDeposit(sender, args);
            case "withdraw" -> handleWithdraw(sender, args);
            // case "top" -> handleTop(sender, args);
            default -> MessageUtil.sendMessage(sender, "invalid-usage", MessageUtil.placeholder("usage", "/" + label + " help"));
        }
        return true;
    }

    private void handleBalance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "console-not-supported");
            return;
        }
        if (!player.hasPermission("kec.balance")) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }

        service.getBankBalance(player.getUniqueId()).thenAccept(bankBalance -> {
            long walletBalance = service.getWalletBalance(player);
            long totalBalance = bankBalance + walletBalance;

            String formattedBank = service.getFormatter().formatWithCommas(bankBalance);
            String formattedWallet = service.getFormatter().formatWithCommas(walletBalance);
            String formattedTotal = service.getFormatter().formatWithCommas(totalBalance);

            MessageUtil.sendRawMessage(player, plugin.getMessagesConfig().getString("balance.header"));
            MessageUtil.sendMessage(player, "balance.bank", MessageUtil.placeholder("balance_bank", formattedBank));
            MessageUtil.sendMessage(player, "balance.wallet", MessageUtil.placeholder("balance_wallet", formattedWallet));
            MessageUtil.sendMessage(player, "balance.total", MessageUtil.placeholder("balance_total", formattedTotal));
            MessageUtil.sendRawMessage(player, plugin.getMessagesConfig().getString("balance.footer"));
        });
    }

    private void handleHelp(CommandSender sender) {
         if (!sender.hasPermission("kec.help")) {
            MessageUtil.sendMessage(sender, "no-permission");
            return;
        }
        // Simple help message for now
        sender.sendMessage("--- KartaEmeraldCurrency Help ---");
        sender.sendMessage("/emerald balance - Check your balance");
        sender.sendMessage("/emerald pay <player> <amount> - Pay a player");
        sender.sendMessage("/emerald deposit <amount|all> - Deposit emeralds");
        sender.sendMessage("/emerald withdraw <amount> - Withdraw emeralds");
        sender.sendMessage("/emerald top - View the leaderboard");
    }

    private void handlePay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "console-not-supported");
            return;
        }
        if (!player.hasPermission("kec.pay")) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }
        // /pay <player> <amount>
        if (args.length < 3) {
            MessageUtil.sendMessage(player, "invalid-usage", MessageUtil.placeholder("usage", "/emerald pay <player> <amount>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "player-not-found", MessageUtil.placeholder("player", args[1]));
            return;
        }
        if (target.equals(player)) {
            MessageUtil.sendMessage(player, "cannot-pay-self");
            return;
        }
        try {
            long amount = Long.parseLong(args[2]);
            if (amount <= 0) {
                MessageUtil.sendMessage(player, "amount-too-low");
                return;
            }
            service.transfer(player.getUniqueId(), target.getUniqueId(), amount, null).thenAccept(success -> {
                if (success) {
                    MessageUtil.sendMessage(player, "pay-success-sender", MessageUtil.placeholder("amount", amount), MessageUtil.placeholder("target", target.getName()));
                    MessageUtil.sendMessage(target, "pay-received-notification", MessageUtil.placeholder("amount", amount), MessageUtil.placeholder("sender", player.getName()));
                } else {
                    MessageUtil.sendMessage(player, "insufficient-funds");
                }
            });
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "invalid-amount");
        }
    }

    private void handleDeposit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "console-not-supported");
            return;
        }
        if (!player.hasPermission("kec.deposit")) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }
        // /deposit <amount>
        if (args.length < 2) {
             MessageUtil.sendMessage(player, "invalid-usage", MessageUtil.placeholder("usage", "/emerald deposit <amount|all>"));
            return;
        }

        // TODO: Handle "all" case

        try {
            long amount = Long.parseLong(args[1]);
            if (amount <= 0) {
                MessageUtil.sendMessage(player, "amount-too-low");
                return;
            }

            service.depositToBank(player.getUniqueId(), amount).thenAccept(success -> {
                if (success) {
                    MessageUtil.sendMessage(player, "deposit-success", MessageUtil.placeholder("amount", amount));
                } else {
                     MessageUtil.sendMessage(player, "insufficient-items");
                }
            });
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "invalid-amount");
        }
    }

    private void handleWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "console-not-supported");
            return;
        }
        if (!player.hasPermission("kec.withdraw")) {
            MessageUtil.sendMessage(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            MessageUtil.sendMessage(player, "invalid-usage", MessageUtil.placeholder("usage", "/emerald withdraw <amount>"));
            return;
        }

        try {
            long amount = Long.parseLong(args[1]);
            if (amount <= 0) {
                MessageUtil.sendMessage(player, "amount-too-low");
                return;
            }

            service.withdrawFromBank(player.getUniqueId(), amount).thenAccept(success -> {
                if (success) {
                    MessageUtil.sendMessage(player, "withdraw-success", MessageUtil.placeholder("amount", amount));
                } else {
                    // Could be insufficient funds or full inventory
                    MessageUtil.sendMessage(player, "insufficient-funds"); // Or inventory-full, need better feedback from service
                }
            });

        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(player, "invalid-amount");
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("balance", "pay", "deposit", "withdraw", "top", "help").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pay")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
