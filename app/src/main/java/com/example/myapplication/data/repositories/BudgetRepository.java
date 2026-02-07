package com.example.myapplication.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.myapplication.data.DatabaseHelper;
import com.example.myapplication.models.Budget;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BudgetRepository {
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public BudgetRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("ExpenseTracker", Context.MODE_PRIVATE);
    }

    private int getCurrentUserId() {
        return prefs.getInt("userId", -1);
    }

    public boolean setBudget(String category, double limit) {
        int userId = getCurrentUserId();
        if (userId <= 0) return false;
        return dbHelper.setBudget(userId, category, limit);
    }

    public List<Budget> getBudgets() {
        int userId = getCurrentUserId();
        if (userId <= 0) return new ArrayList<>();
        
        try {
            String json = dbHelper.getBudgets(userId);
            JSONArray jsonArray = new JSONArray(json);
            List<Budget> budgets = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                Budget budget = new Budget(
                    obj.getString("category"),
                    obj.getDouble("limit")
                );
                budgets.add(budget);
            }
            return budgets;
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }
    public boolean deleteBudget(String category) {
        int userId = getCurrentUserId();
        if (userId <= 0) return false;
        return dbHelper.deleteBudget(userId, category);
    }
}
