package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.webkit.JavascriptInterface;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WebAppInterface {
    private Context context;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public WebAppInterface(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("ExpenseTracker", Context.MODE_PRIVATE);
    }

    @JavascriptInterface
    public String login(String username, String password) {
        try {
            DatabaseHelper.User user = dbHelper.login(username, password);
            if (user != null) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("userId", user.id);
                editor.putString("username", user.username);
                editor.apply();

                JSONObject result = new JSONObject();
                result.put("ok", true);
                JSONObject userObj = new JSONObject();
                userObj.put("id", user.id);
                userObj.put("username", user.username);
                result.put("user", userObj);
                return result.toString();
            } else {
                JSONObject result = new JSONObject();
                result.put("ok", false);
                result.put("error", "Invalid username or password");
                return result.toString();
            }
        } catch (Exception e) {
            Log.e("WebAppInterface", "Login error", e);
            try {
                JSONObject result = new JSONObject();
                result.put("ok", false);
                result.put("error", "Login failed");
                return result.toString();
            } catch (JSONException je) {
                return "{\"ok\":false,\"error\":\"Login failed\"}";
            }
        }
    }

    @JavascriptInterface
    public String signup(String username, String password, String pet) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return "{\"ok\":false,\"error\":\"Username is required\"}";
            }
            if (password == null || password.length() < 3) {
                return "{\"ok\":false,\"error\":\"Password must be at least 3 characters\"}";
            }
            if (pet == null || pet.trim().isEmpty()) {
                return "{\"ok\":false,\"error\":\"Security answer is required\"}";
            }

            long userId = dbHelper.signup(username, password, pet);
            if (userId > 0) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("userId", (int) userId);
                editor.putString("username", username);
                editor.apply();

                JSONObject result = new JSONObject();
                result.put("ok", true);
                JSONObject userObj = new JSONObject();
                userObj.put("id", userId);
                userObj.put("username", username);
                result.put("user", userObj);
                return result.toString();
            } else {
                return "{\"ok\":false,\"error\":\"Username already exists\"}";
            }
        } catch (Exception e) {
            Log.e("WebAppInterface", "Signup error", e);
            return "{\"ok\":false,\"error\":\"Signup failed\"}";
        }
    }

    @JavascriptInterface
    public String resetPassword(String username, String pet, String newPassword) {
        try {
            if (newPassword == null || newPassword.length() < 3) {
                return "{\"ok\":false,\"error\":\"Password must be at least 3 characters\"}";
            }

            boolean success = dbHelper.resetPassword(username, pet, newPassword);
            JSONObject result = new JSONObject();
            result.put("ok", success);
            if (!success) {
                result.put("error", "Invalid username or security answer");
            }
            return result.toString();
        } catch (Exception e) {
            Log.e("WebAppInterface", "Reset password error", e);
            return "{\"ok\":false,\"error\":\"Reset failed\"}";
        }
    }

    @JavascriptInterface
    public String getCurrentUser() {
        try {
            int userId = prefs.getInt("userId", -1);
            String username = prefs.getString("username", null);
            if (userId > 0 && username != null) {
                JSONObject user = new JSONObject();
                user.put("id", userId);
                user.put("username", username);
                return user.toString();
            }
            return null;
        } catch (Exception e) {
            Log.e("WebAppInterface", "Get current user error", e);
            return null;
        }
    }

    @JavascriptInterface
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("userId");
        editor.remove("username");
        editor.apply();
    }

    @JavascriptInterface
    public long addExpense(String category, double amount, String note, String date) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return -1;
        return dbHelper.addExpense(userId, category, amount, note, date);
    }

    @JavascriptInterface
    public String getExpenses() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return "[]";
        return dbHelper.getExpenses(userId);
    }

    @JavascriptInterface
    public boolean updateExpense(int expenseId, String category, double amount, String note, String date) {
        return dbHelper.updateExpense(expenseId, category, amount, note, date);
    }

    @JavascriptInterface
    public boolean deleteExpense(int expenseId) {
        return dbHelper.deleteExpense(expenseId);
    }

    @JavascriptInterface
    public boolean clearExpenses() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.clearExpenses(userId);
    }

    @JavascriptInterface
    public boolean setBudget(String category, double limit) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.setBudget(userId, category, limit);
    }

    @JavascriptInterface
    public String getBudgets() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return "[]";
        return dbHelper.getBudgets(userId);
    }

    @JavascriptInterface
    public boolean deleteBudget(String category) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.deleteBudget(userId, category);
    }

    @JavascriptInterface
    public boolean exportCsv() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;

        try {
            String expensesJson = dbHelper.getExpenses(userId);
            if (expensesJson == null || expensesJson.equals("[]")) {
                return false;
            }

            org.json.JSONArray expenses = new org.json.JSONArray(expensesJson);
            StringBuilder csv = new StringBuilder();
            csv.append("ID,Category,Amount,Note,Date\n");

            for (int i = 0; i < expenses.length(); i++) {
                org.json.JSONObject exp = expenses.getJSONObject(i);
                csv.append(exp.getInt("id")).append(",")
                    .append(exp.getString("category")).append(",")
                    .append(exp.getDouble("amount")).append(",")
                    .append("\"").append(exp.getString("note").replace("\"", "\"\"")).append("\",")
                    .append("\"").append(exp.getString("date")).append("\"\n");
            }

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadsDir, "expenses_" + System.currentTimeMillis() + ".csv");
            FileWriter writer = new FileWriter(csvFile);
            writer.write(csv.toString());
            writer.close();

            return true;
        } catch (Exception e) {
            Log.e("WebAppInterface", "Export CSV error", e);
            return false;
        }
    }

    @JavascriptInterface
    public String getAppInfo() {
        try {
            JSONObject info = new JSONObject();
            info.put("name", "Expense Tracker");
            info.put("versionName", "1.0");
            info.put("security", "All passwords and security answers are hashed using SHA-256 and stored securely on-device.");
            return info.toString();
        } catch (Exception e) {
            Log.e("WebAppInterface", "Get app info error", e);
            return null;
        }
    }
}
