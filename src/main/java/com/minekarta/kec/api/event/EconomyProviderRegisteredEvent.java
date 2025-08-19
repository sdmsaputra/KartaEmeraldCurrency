package com.minekarta.kec.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when KartaEmeraldCurrency successfully registers its economy provider with Vault.
 * This is a signal that the economy is ready to be used through the Vault API.
 */
public class EconomyProviderRegisteredEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final String economyName;

    /**
     * Constructs a new EconomyProviderRegisteredEvent.
     *
     * @param economyName The name of the economy provider.
     */
    public EconomyProviderRegisteredEvent(@NotNull String economyName) {
        super(true); // This can be async as it's just a notification
        this.economyName = economyName;
    }

    /**
     * Gets the name of the economy provider that was registered.
     * @return The economy provider name.
     */
    @NotNull
    public String getEconomyName() {
        return economyName;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Gets the handler list for this event.
     * @return The handler list.
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
