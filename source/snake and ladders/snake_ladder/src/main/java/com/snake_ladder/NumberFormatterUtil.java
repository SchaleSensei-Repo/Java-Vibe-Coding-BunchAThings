package com.snake_ladder; // Or your package

import java.text.DecimalFormat;

public class NumberFormatterUtil {

    // Suffixes for K (kilo), M (mega), B (giga), T (tera)
    private static final char[] SUFFIXES = {' ', 'k', 'M', 'B', 'T'}; // Space for < 1000
    private static final int MAX_LENGTH = 4; // Max length of the formatted string (e.g., "999k", "1.2M")

    public static String formatNumberShort(long number) {
        if (number == 0) {
            return "0";
        }
        if (number < 0) { // Handle negative numbers by formatting their absolute value and prepending "-"
            return "-" + formatNumberShort(Math.abs(number));
        }

        // Using a simpler approach for K and M for game context
        if (number < 1000) {
            return String.valueOf(number); // No suffix for numbers less than 1000
        } else if (number < 1_000_000) { // Thousands (K)
            if (number % 1000 == 0) { // e.g., 2000 -> 2k
                return (number / 1000) + "k";
            } else if (number < 10_000) { // e.g., 3345 -> 3k, 9999 -> 9k
                 return (number / 1000) + "k"; // Simple truncation for this case
            } else { // e.g., 33452 -> 33k, 125000 -> 125k
                return (number / 1000) + "k";
            }
        } else { // Millions (M)
            if (number % 1_000_000 == 0) { // e.g., 3000000 -> 3M
                return (number / 1_000_000) + "M";
            } else if (number < 10_000_000) { // e.g., 3345200 -> 3M
                 return (number / 1_000_000) + "M";
            }
             else { // e.g., 125000000 -> 125M
                return (number / 1_000_000) + "M";
            }
        }
        // Fallback for very large numbers not covered (or extend with B, T etc.)
        // For this game, M should be sufficient.
        // return String.valueOf(number); 
    }

    // More generic version if you need more precision or larger suffixes (B, T)
    // This is just an example and might be overkill for the current game tile display
    public static String formatNumberGeneric(long number) {
        if (number == 0) return "0";
        if (number < 1000 && number > -1000) return String.valueOf(number);

        String r = new DecimalFormat("##0.##").format(number).replaceFirst("\\.0+$", "");
        int E = (int) Math.floor(Math.log10(Math.abs(number)) / 3);
        if (E > 0 && E < SUFFIXES.length) {
            // Divide number by 1000^E
            double value = number / Math.pow(1000, E);
            // Format with one decimal place if not an integer, or no decimal place if integer
            DecimalFormat df = (value == Math.floor(value)) ? new DecimalFormat("#0") : new DecimalFormat("#0.0");
            r = df.format(value) + SUFFIXES[E];
        }
        return r;
    }
}