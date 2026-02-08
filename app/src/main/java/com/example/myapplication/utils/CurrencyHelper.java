package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyHelper {
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_CURRENCY = "currency_code";

    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_KHR = "KHR";
    public static final String CURRENCY_CNY = "CNY";
    public static final String CURRENCY_VND = "VND";

    public static String getCurrencyCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CURRENCY, CURRENCY_USD);
    }

    public static void setCurrencyCode(Context context, String currencyCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY, currencyCode).apply();
    }

    public static String getCurrencySymbol(Context context) {
        String code = getCurrencyCode(context);
        switch (code) {
            case CURRENCY_KHR: return "៛";
            case CURRENCY_CNY: return "¥";
            case CURRENCY_VND: return "₫";
            case CURRENCY_USD: 
            default: return "$";
        }
    }

    public static String formatCurrency(Context context, double amount) {
        String code = getCurrencyCode(context);
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        
        // Remove default symbol
        String formattedNumber = format.format(amount).replace("$", "").trim();

        // Custom formatting based on currency
        switch (code) {
            case CURRENCY_KHR:
                // Khmer Riel usually doesn't need decimals for amounts visible in normal transactions
                return String.format(Locale.US, "%,.0f ៛", amount); 
            case CURRENCY_CNY:
                return "¥" + formattedNumber;
            case CURRENCY_VND:
                return String.format(Locale.US, "%,.0f ₫", amount);
            case CURRENCY_USD:
            default:
                return "$" + formattedNumber;
        }
    }
}
