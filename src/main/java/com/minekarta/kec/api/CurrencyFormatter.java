package com.minekarta.kec.api;

import org.jetbrains.annotations.NotNull;

/**
 * A utility for formatting currency amounts into human-readable strings.
 * An instance of this formatter can be retrieved from {@link KartaEmeraldService#getFormatter()}.
 */
public interface CurrencyFormatter {

    /**
     * Formats a raw long value into a string with thousand separators (e.g., 12345 becomes "12,345").
     *
     * @param amount The amount to format.
     * @return A string representation of the amount with commas.
     */
    @NotNull
    String formatWithCommas(long amount);

    /**
     * Formats a raw long value into a compact string representation (e.g., 1234 becomes "1.2k").
     * The exact formatting depends on the plugin's configuration.
     *
     * @param amount The amount to format.
     * @return A compact string representation of the amount.
     */
    @NotNull
    String formatCompact(long amount);

    /**
     * Formats a raw long value according to the default format specified in the plugin's configuration.
     * This could be compact or with commas.
     *
     * @param amount The amount to format.
     * @return A formatted string representation of the amount.
     */
    @NotNull
    String formatDefault(long amount);

}
