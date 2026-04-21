package com.cinemora.movieorder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Centralized utility class for formatting timestamps and currency values.
 * Ensures consistency across the entire application.
 */
public class DateUtils {

    /**
     * Formats a Unix timestamp (in seconds) to "YYYY-MM-DD" format.
     * Used for movie release dates.
     *
     * @param seconds Unix timestamp in seconds
     * @return Formatted date string, e.g., "2024-12-25"
     */
    public static String formatReleaseDate(long seconds) {
        if (seconds == 0) return "N/A";
        try {
            Date date = new Date(seconds * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Formats a Unix timestamp (in seconds) to "DD MMM YYYY HH:mm" format.
     * Used for order dates and other timestamps.
     * Example: "25 Dec 2024 14:30"
     *
     * @param seconds Unix timestamp in seconds
     * @return Formatted date-time string
     */
    public static String formatOrderDate(long seconds) {
        if (seconds == 0) return "N/A";
        try {
            Date date = new Date(seconds * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Formats an integer cost value (in HKD) to "HK$XXX" format.
     * Used for all cost-related fields (prices, totals, credits, etc.)
     *
     * @param hkd Cost in HKD (integer)
     * @return Formatted currency string, e.g., "HK$130"
     */
    public static String formatCurrency(int hkd) {
        return String.format(Locale.getDefault(), "HK$%d", hkd);
    }

    /**
     * Converts Unix timestamp (seconds) to current system Date object.
     * Useful for internal calculations.
     *
     * @param seconds Unix timestamp in seconds
     * @return Date object
     */
    public static Date toDate(long seconds) {
        return new Date(seconds * 1000);
    }

    /**
     * Converts current time to Unix timestamp (seconds).
     * Used when creating new documents in Firestore.
     *
     * @return Current time as Unix timestamp in seconds
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }
}

