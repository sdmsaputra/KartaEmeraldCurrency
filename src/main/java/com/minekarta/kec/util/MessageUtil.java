package com.minekarta.kec.util;

import com.minekarta.kec.KartaEmeraldCurrencyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * A utility class for sending messages to players.
 */
public class MessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static String prefix = "";

    /**
     * Private constructor to prevent instantiation.
     */
    private MessageUtil() {
        // Utility class
    }

    /**
     * Loads the message prefix from the configuration.
     * @param plugin The plugin instance.
     */
    public static void load(KartaEmeraldCurrencyPlugin plugin) {
        FileConfiguration messages = plugin.getMessagesConfig();
        prefix = messages.getString("prefix", "<dark_gray>[<green>KEC</green>]<reset> ");
    }

    /**
     * Sends a message from messages.yml to a target.
     * @param target The target to send the message to.
     * @param messageKey The key of the message in messages.yml.
     * @param resolvers Placeholders to use in the message.
     */
    public static void sendMessage(CommandSender target, String messageKey, TagResolver... resolvers) {
        KartaEmeraldCurrencyPlugin plugin = KartaEmeraldCurrencyPlugin.getInstance();
        String message = plugin.getMessagesConfig().getString(messageKey, "<red>Unknown message key: " + messageKey + "</red>");

        Component parsedMessage = miniMessage.deserialize(prefix + message, resolvers);
        target.sendMessage(parsedMessage);
    }

    /**
     * Sends a raw string message to a target.
     * @param target The target to send the message to.
     * @param message The message to send.
     * @param resolvers Placeholders to use in the message.
     */
    public static void sendRawMessage(CommandSender target, String message, TagResolver... resolvers) {
        Component parsedMessage = miniMessage.deserialize(message, resolvers);
        target.sendMessage(parsedMessage);
    }

    /**
     * Creates a placeholder for use in messages.
     * @param key The placeholder key.
     * @param value The placeholder value.
     * @return The created TagResolver.
     */
    public static TagResolver placeholder(String key, String value) {
        return Placeholder.unparsed(key, value);
    }

    /**
     * Creates a placeholder for use in messages.
     * @param key The placeholder key.
     * @param value The placeholder value.
     * @return The created TagResolver.
     */
    public static TagResolver placeholder(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }
}
