package com.diamondrush.util;

import java.util.Locale;

/**
 * Small formatting helpers shared by the server logic and the client GUI.
 */
public final class Format {

    private Format() {
    }

    /**
     * Formats a duration in milliseconds as MM:SS (minutes are not capped at 99).
     */
    public static String clock(long millis) {
        long totalSeconds = Math.max(0L, millis) / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    /**
     * Formats a duration in milliseconds as a compact "12.4s" style string.
     */
    public static String seconds(long millis) {
        return String.format(Locale.ROOT, "%.1fs", Math.max(0L, millis) / 1000.0D);
    }
}
