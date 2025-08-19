package com.minekarta.kec.util;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class MessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static String prefix = "";

    public static void load(KartaEmeraldCurrencyPlugin plugin) {
        FileConfiguration messages = plugin.getMessagesConfig();
        prefix = messages.getString("prefix", "<dark_gray>[<green>KEC</green>]<reset> ");
    }

    public static void sendMessage(CommandSender target, String messageKey, TagResolver... resolvers) {
        KartaEmeraldCurrencyPlugin plugin = KartaEmeraldCurrencyPlugin.getInstance();
        String message = plugin.getMessagesConfig().getString(messageKey, "<red>Unknown message key: " + messageKey + "</red>");

        Component parsedMessage = miniMessage.deserialize(prefix + message, resolvers);
        target.sendMessage(parsedMessage);
    }

    public static void sendRawMessage(CommandSender target, String message, TagResolver... resolvers) {
        Component parsedMessage = miniMessage.deserialize(message, resolvers);
        target.sendMessage(parsedMessage);
    }

    public static TagResolver placeholder(String key, String value) {
        return Placeholder.unparsed(key, value);
    }

    public static TagResolver placeholder(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
}
