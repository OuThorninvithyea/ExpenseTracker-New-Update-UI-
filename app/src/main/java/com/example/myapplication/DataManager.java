package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * DataManager - Singleton class that manages all data operations for the Expense Tracker app.
 * 
 * This class acts as a facade between the UI and database, providing:
 * - Singleton pattern to ensure only one instance exists
 * - User authentication (login, signup, password management)
 * - Expense CRUD operations
 * - Budget management
 * - Budget checking before expense operations
 * - Guest user management (app runs without login/signup)
 * 
 * Uses SharedPreferences to store current user session information.
 * Delegates database operations to DatabaseHelper.
 */
public class DataManager {
    // Singleton instance - ensures only one DataManager exists throughout app lifecycle
    private static DataManager instance;
    
    // Database helper for all SQLite operations
    private DatabaseHelper dbHelper;
    
    // SharedPreferences for storing user session and app preferences
    private SharedPreferences prefs;
    
    // Application context
    private Context context;

    /**
     * Private constructor - prevents direct instantiation.
     * Use getInstance() to get the singleton instance.
     * 
     * @param context Application context
     */
    private DataManager(Context context) {
        this.context = context;
        // Initialize SharedPreferences for storing user session data
        this.prefs = context.getSharedPreferences("ExpenseTracker", Context.MODE_PRIVATE);
        
        // Initialize database helper with error recovery
        try {
            this.dbHelper = new DatabaseHelper(context);
            android.util.Log.d("DataManager", "DatabaseHelper initialized");
        } catch (Exception e) {
            android.util.Log.e("DataManager", "Error initializing DatabaseHelper: " + e.getMessage(), e);
            // Try to recover by deleting and recreating database
            try {
                context.deleteDatabase("expense_tracker.db");
                this.dbHelper = new DatabaseHelper(context);
            } catch (Exception e2) {
                android.util.Log.e("DataManager", "Failed to recover database: " + e2.getMessage(), e2);
            }
        }

        // App runs without login/signup: ensure we always have a valid user
        // This creates a Guest user if none exists
        ensureGuestUser();
    }

    /**
     * Ensures a Guest user exists and is stored in SharedPreferences.
     * Called during initialization to guarantee the app always has a valid user.
     */
    private void ensureGuestUser() {
        int userId = prefs.getInt("userId", -1);
        if (userId > 0) return;
        if (dbHelper == null) return;

        int guestId = dbHelper.ensureGuestUser();
        if (guestId > 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("userId", guestId);
            editor.putString("username", "Guest");
            editor.apply();
        }
    }
    
    /**
     * Completely resets the database and clears all stored preferences.
     * Use with caution - this deletes all user data.
     */
    public void resetDatabase() {
        android.util.Log.d("DataManager", "Resetting database via DataManager...");
        // Clear SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        
        // Reset database
        dbHelper.resetDatabase(context);
        
        android.util.Log.d("DataManager", "Database reset completed");
    }

    /**
     * Gets the singleton instance of DataManager.
     * Creates a new instance if one doesn't exist.
     * Thread-safe using synchronized keyword.
     * 
     * @param context Application context
     * @return The singleton DataManager instance
     */
    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            // Use application context to prevent memory leaks
            instance = new DataManager(context.getApplicationContext());
        }
        return instance;
    }

    // ==================== Authentication Methods ====================
    
    /**
     * Authenticates a user with username and password.
     * On success, saves user session to SharedPreferences.
     * 
     * @param username The username to authenticate
     * @param password The password to verify
     * @return LoginResult containing success status, user object, and error message
     */
    public LoginResult login(String username, String password) {
        android.util.Log.d("DataManager", "Login attempt for: " + username);
        DatabaseHelper.User user = dbHelper.login(username, password);
        if (user != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("userId", user.id);
            editor.putString("username", user.username);
            editor.apply();
            android.util.Log.d("DataManager", "Login success, saving user to prefs");
            return new LoginResult(true, user, null);
        }
        android.util.Log.e("DataManager", "Login failed for: " + username);
        return new LoginResult(false, null, "Invalid username or password");
    }

    /**
     * Creates a new user account.
     * Validates inputs and checks for duplicate usernames.
     * On success, saves user session to SharedPreferences.
     * 
     * @param username The desired username (must be unique)
     * @param password The password (must be at least 3 characters)
     * @param pet The security question answer
     * @return SignupResult containing success status, user object, and error message
     */
    public SignupResult signup(String username, String password, String pet) {
        android.util.Log.d("DataManager", "Signup attempt for: " + username);
        if (username == null || username.trim().isEmpty()) {
            android.util.Log.e("DataManager", "Signup failed: Username is required");
            return new SignupResult(false, null, "Username is required");
        }
        if (password == null || password.length() < 3) {
            android.util.Log.e("DataManager", "Signup failed: Password too short");
            return new SignupResult(false, null, "Password must be at least 3 characters");
        }
        if (pet == null || pet.trim().isEmpty()) {
            android.util.Log.e("DataManager", "Signup failed: Security answer required");
            return new SignupResult(false, null, "Security answer is required");
        }

        long userId = dbHelper.signup(username, password, pet);
        if (userId > 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("userId", (int) userId);
            editor.putString("username", username.trim());
            editor.apply();
            android.util.Log.d("DataManager", "Signup success, user ID: " + userId);
            DatabaseHelper.User user = new DatabaseHelper.User((int) userId, username.trim());
            return new SignupResult(true, user, null);
        }
        android.util.Log.e("DataManager", "Signup failed: Database returned userId: " + userId);
        // Check if it's a duplicate username or other error
        if (userId == -2) {
            return new SignupResult(false, null, "Username already exists. Please choose a different username.");
        } else if (userId == -1) {
            return new SignupResult(false, null, "Database error occurred. Please try again.");
        }
        return new SignupResult(false, null, "Signup failed. Please try again.");
    }

    /**
     * Resets a user's password using security question answer.
     * 
     * @param username The username
     * @param pet The security question answer
     * @param newPassword The new password (must be at least 3 characters)
     * @return true if reset successful, false otherwise
     */
    public boolean resetPassword(String username, String pet, String newPassword) {
        if (newPassword == null || newPassword.length() < 3) {
            return false;
        }
        return dbHelper.resetPassword(username, pet, newPassword);
    }
    
    /**
     * Updates the current user's username.
     * Also updates the username in SharedPreferences.
     * 
     * @param newUsername The new username (must be unique)
     * @return true if update successful, false otherwise
     */
    public boolean updateUsername(String newUsername) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return false;
        }
        
        boolean success = dbHelper.updateUsername(userId, newUsername);
        if (success) {
            // Update SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", newUsername.trim());
            editor.apply();
        }
        return success;
    }
    
    /**
     * Updates the current user's password.
     * Requires current password for verification.
     * 
     * @param currentPassword The user's current password
     * @param newPassword The new password (must be at least 3 characters)
     * @return true if update successful, false otherwise
     */
    public boolean updatePassword(String currentPassword, String newPassword) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        
        if (newPassword == null || newPassword.length() < 3) {
            return false;
        }
        
        return dbHelper.updatePassword(userId, currentPassword, newPassword);
    }

    /**
     * Gets the currently logged-in user from SharedPreferences.
     * Ensures Guest user exists if no user is found.
     * 
     * @return User object representing the current user, or null if error
     */
    public DatabaseHelper.User getCurrentUser() {
        // Ensure a user exists even if user cleared prefs
        ensureGuestUser();
        int userId = prefs.getInt("userId", -1);
        String username = prefs.getString("username", null);
        if (userId > 0 && username != null) {
            return new DatabaseHelper.User(userId, username);
        }
        return null;
    }

    /**
     * Logs out the current user by clearing session data from SharedPreferences.
     * Note: The app will automatically create a Guest user on next access.
     */
    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("userId");
        editor.remove("username");
        editor.apply();
    }

    // ==================== Expense Methods ====================
    
    /**
     * Adds a new expense for the current user.
     * 
     * @param category The expense category
     * @param amount The expense amount
     * @param note Optional note/description
     * @param date The date of the expense
     * @return The ID of the newly created expense, or -1 if failed
     */
    public long addExpense(String category, double amount, String note, String date) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return -1;
        return dbHelper.addExpense(userId, category, amount, note, date);
    }

    /**
     * Retrieves all expenses for the current user.
     * Converts JSON string from database to List of Expense objects.
     * 
     * @return List of Expense objects, empty list if none found or error occurs
     */
    public List<Expense> getExpenses() {
        int userId = prefs.getInt("userId", -1);
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

    /**
     * Updates an existing expense.
     * 
     * @param expenseId The ID of the expense to update
     * @param category The new category
     * @param amount The new amount
     * @param note The new note
     * @param date The new date
     * @return true if update successful, false otherwise
     */
    public boolean updateExpense(int expenseId, String category, double amount, String note, String date) {
        return dbHelper.updateExpense(expenseId, category, amount, note, date);
    }

    /**
     * Deletes an expense.
     * 
     * @param expenseId The ID of the expense to delete
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteExpense(int expenseId) {
        return dbHelper.deleteExpense(expenseId);
    }

    /**
     * Deletes all expenses for the current user.
     * 
     * @return true if operation successful, false otherwise
     */
    public boolean clearExpenses() {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.clearExpenses(userId);
    }

    // ==================== Budget Methods ====================
    
    /**
     * Sets or updates a budget limit for a category.
     * 
     * @param category The category name
     * @param limit The budget limit amount
     * @return true if operation successful, false otherwise
     */
    public boolean setBudget(String category, double limit) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.setBudget(userId, category, limit);
    }

    /**
     * Retrieves all budgets for the current user.
     * Converts JSON string from database to List of Budget objects.
     * 
     * @return List of Budget objects, empty list if none found or error occurs
     */
    public List<Budget> getBudgets() {
        int userId = prefs.getInt("userId", -1);
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

    /**
     * Deletes a budget for a specific category.
     * 
     * @param category The category name
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteBudget(String category) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.deleteBudget(userId, category);
    }

    // ==================== Budget Checking Methods ====================
    
    /**
     * Checks if adding a new expense would exceed the budget for a category.
     * Calculates current total spent and compares with budget limit.
     * 
     * @param category The expense category
     * @param amount The expense amount to check
     * @return BudgetCheckResult containing whether budget would be exceeded and related amounts
     */
    public BudgetCheckResult checkBudget(String category, double amount) {
        List<Budget> budgets = getBudgets();
        
        // Find budget for this category
        DataManager.Budget budget = null;
        for (DataManager.Budget b : budgets) {
            if (b.category.equals(category)) {
                budget = b;
                break;
            }
        }
        
        // If no budget set for this category, no check needed
        if (budget == null) {
            return new BudgetCheckResult(false, 0, 0, 0);
        }
        
        // Calculate current total spent for this category
        List<Expense> expenses = getExpenses();
        double totalSpent = 0;
        for (Expense expense : expenses) {
            if (expense.category.equals(category)) {
                totalSpent += expense.amount;
            }
        }
        
        // Calculate new total if this expense is added
        double newTotal = totalSpent + amount;
        boolean exceedsBudget = newTotal >= budget.limit;
        
        return new BudgetCheckResult(exceedsBudget, budget.limit, totalSpent, newTotal);
    }

    /**
     * Checks if updating an expense would exceed the budget.
     * Excludes the expense being updated from the current total calculation.
     * 
     * @param category The expense category
     * @param newAmount The new expense amount to check
     * @param expenseId The ID of the expense being updated (excluded from current total)
     * @return BudgetCheckResult containing whether budget would be exceeded and related amounts
     */
    public BudgetCheckResult checkBudgetOnUpdate(String category, double newAmount, int expenseId) {
        List<Budget> budgets = getBudgets();
        
        // Find budget for this category
        DataManager.Budget budget = null;
        for (DataManager.Budget b : budgets) {
            if (b.category.equals(category)) {
                budget = b;
                break;
            }
        }
        
        // If no budget set for this category, no check needed
        if (budget == null) {
            return new BudgetCheckResult(false, 0, 0, 0);
        }
        
        // Calculate current total spent for this category (excluding the expense being updated)
        List<Expense> expenses = getExpenses();
        double totalSpent = 0;
        for (Expense expense : expenses) {
            if (expense.category.equals(category) && expense.id != expenseId) {
                totalSpent += expense.amount;
            }
        }
        
        // Add new amount
        double newTotal = totalSpent + newAmount;
        boolean exceedsBudget = newTotal >= budget.limit;
        
        return new BudgetCheckResult(exceedsBudget, budget.limit, totalSpent, newTotal);
    }

    // ==================== Result Classes ====================
    
    /**
     * Result class for login operations.
     * Contains success status, user object (if successful), and error message (if failed).
     */
    public static class LoginResult {
        public boolean success;
        public DatabaseHelper.User user;
        public String error;

        public LoginResult(boolean success, DatabaseHelper.User user, String error) {
            this.success = success;
            this.user = user;
            this.error = error;
        }
    }

    /**
     * Result class for signup operations.
     * Contains success status, user object (if successful), and error message (if failed).
     */
    public static class SignupResult {
        public boolean success;
        public DatabaseHelper.User user;
        public String error;

        public SignupResult(boolean success, DatabaseHelper.User user, String error) {
            this.success = success;
            this.user = user;
            this.error = error;
        }
    }

    // ==================== Data Classes ====================
    
    /**
     * Data class representing an expense transaction.
     */
    public static class Expense {
        public int id;
        public String category;
        public double amount;
        public String note;
        public String date;

        public Expense(int id, String category, double amount, String note, String date) {
            this.id = id;
            this.category = category;
            this.amount = amount;
            this.note = note;
            this.date = date;
        }
    }

    /**
     * Data class representing a budget limit for a category.
     */
    public static class Budget {
        public String category;
        public double limit;

        public Budget(String category, double limit) {
            this.category = category;
            this.limit = limit;
        }
    }

    /**
     * Result class for budget checking operations.
     * Contains information about whether a budget would be exceeded and related amounts.
     */
    public static class BudgetCheckResult {
        public boolean exceedsBudget;
        public double budgetLimit;
        public double currentSpent;
        public double newTotal;

        public BudgetCheckResult(boolean exceedsBudget, double budgetLimit, double currentSpent, double newTotal) {
            this.exceedsBudget = exceedsBudget;
            this.budgetLimit = budgetLimit;
            this.currentSpent = currentSpent;
            this.newTotal = newTotal;
        }
    }
}
