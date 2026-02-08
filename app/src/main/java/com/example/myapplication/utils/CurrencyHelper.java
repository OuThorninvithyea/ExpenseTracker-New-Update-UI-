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

    public static double getExchangeRate(Context context) {
        String code = getCurrencyCode(context);
        switch (code) {
            case CURRENCY_KHR: return 4100.0;
            case CURRENCY_CNY: return 7.2;
            case CURRENCY_VND: return 25450.0;
            case CURRENCY_USD: 
            default: return 1.0;
        }
    }

    public static double convertUsdToSelected(Context context, double usdAmount) {
        return usdAmount * getExchangeRate(context);
    }

    public static double convertSelectedToUsd(Context context, double selectedAmount) {
        double rate = getExchangeRate(context);
        return rate > 0 ? selectedAmount / rate : selectedAmount;
    }

    public static String formatCurrency(Context context, double usdAmount) {
        String code = getCurrencyCode(context);
        double convertedAmount = convertUsdToSelected(context, usdAmount);
        
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        
        // Remove default symbol
        String formattedNumber = format.format(convertedAmount).replace("$", "").trim();

        // Custom formatting based on currency
        switch (code) {
            case CURRENCY_KHR:
                // Khmer Riel usually doesn't need decimals for amounts visible in normal transactions
                return String.format(Locale.US, "%,.0f ៛", convertedAmount); 
            case CURRENCY_CNY:
                return "¥" + formattedNumber;
            case CURRENCY_VND:
                return String.format(Locale.US, "%,.0f ₫", convertedAmount);
            case CURRENCY_USD:
            default:
                return "$" + formattedNumber;
        }
    }
}
