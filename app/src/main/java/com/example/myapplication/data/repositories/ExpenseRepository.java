package com.example.myapplication.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.myapplication.data.DatabaseHelper;
import com.example.myapplication.models.Expense;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ExpenseRepository {
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public ExpenseRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("ExpenseTracker", Context.MODE_PRIVATE);
    }

    private int getCurrentUserId() {
        return prefs.getInt("userId", -1);
    }

    public long addExpense(String category, double amount, String note, String date) {
        int userId = getCurrentUserId();
        if (userId <= 0) return -1;
        return dbHelper.addExpense(userId, category, amount, note, date);
    }

    public List<Expense> getExpenses() {
        int userId = getCurrentUserId();
        if (userId <= 0) return new ArrayList<>();
        
        try {
            String json = dbHelper.getExpenses(userId);
            JSONArray jsonArray = new JSONArray(json);
            List<Expense> expenses = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Expense expense = new Expense(
                    obj.getInt("id"),
                    obj.getString("category"),
                    obj.getDouble("amount"),
                    obj.getString("note"),
                    obj.getString("date")
                );
                expenses.add(expense);
            }
            return expenses;
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    public boolean updateExpense(int expenseId, String category, double amount, String note, String date) {
        return dbHelper.updateExpense(expenseId, category, amount, note, date);
    }

    public boolean deleteExpense(int expenseId) {
        return dbHelper.deleteExpense(expenseId);
    }

    public boolean clearExpenses() {
        int userId = getCurrentUserId();
        if (userId <= 0) return false;
        return dbHelper.clearExpenses(userId);
    }
}
