package com.minekarta.kec.command;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import com.minekarta.kec.api.KartaEmeraldService;
import com.minekarta.kec.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EmeraldAdminCommand implements CommandExecutor, TabCompleter {

    private final KartaEmeraldCurrencyPlugin plugin;
    private final KartaEmeraldService service;

    public EmeraldAdminCommand(KartaEmeraldCurrencyPlugin plugin) {
        this.plugin = plugin;
        this.service = plugin.getService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "invalid-usage", MessageUtil.placeholder("usage", "/" + label + " help"));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        // Player-targeting commands require args <player> <amount>
        if (Arrays.asList("set", "add", "remove", "give", "take").contains(subCommand)) {
             if (args.length < 3) {
                MessageUtil.sendMessage(sender, "invalid-usage", MessageUtil.placeholder("usage", "/" + label + " " + subCommand + " <player> <amount>"));
                return true;
            }
            handlePlayerTargetCommand(sender, subCommand, args);
            return true;
        }

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            // case "migrate" -> handleMigrate(sender, args);
            default -> MessageUtil.sendMessage(sender, "invalid-usage", MessageUtil.placeholder("usage", "/" + label + " help"));
        }

        return true;
    }

    private void handlePlayerTargetCommand(CommandSender sender, String subCommand, String[] args) {
        String permission = "kec.admin." + subCommand;
        if (!sender.hasPermission(permission)) {
            MessageUtil.sendMessage(sender, "no-permission");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
             MessageUtil.sendMessage(sender, "player-not-found", MessageUtil.placeholder("player", args[1]));
            return;
        }
        UUID targetId = target.getUniqueId();

        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount <= 0) {
                MessageUtil.sendMessage(sender, "amount-too-low");
                return;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "invalid-amount");
            return;
        }

        switch(subCommand) {
            case "set" -> service.setBankBalance(targetId, amount).thenRun(() ->
                MessageUtil.sendMessage(sender, "balance-set", MessageUtil.placeholder("player", target.getName()), MessageUtil.placeholder("amount", amount)));
            case "add" -> service.addBankBalance(targetId, amount).thenRun(() ->
                MessageUtil.sendMessage(sender, "balance-add", MessageUtil.placeholder("player", target.getName()), MessageUtil.placeholder("amount", amount)));
            case "remove" -> service.removeBankBalance(targetId, amount).thenRun(() ->
                MessageUtil.sendMessage(sender, "balance-remove", MessageUtil.placeholder("player", target.getName()), MessageUtil.placeholder("amount", amount)));
            // TODO: give and take commands
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("kec.admin.reload")) {
            MessageUtil.sendMessage(sender, "no-permission");
            return;
        }
        // This is a simplified reload. A proper reload is much more complex.
        plugin.reloadConfig();
        // custom configs need manual reload
        MessageUtil.load(plugin);
        MessageUtil.sendMessage(sender, "reload-success");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "add", "remove", "give", "take", "reload", "migrate").stream()
                    .filter(s -> sender.hasPermission("kec.admin." + s))
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && Arrays.asList("set", "add", "remove", "give", "take").contains(args[0].toLowerCase())) {
             return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return null;
    }
}
