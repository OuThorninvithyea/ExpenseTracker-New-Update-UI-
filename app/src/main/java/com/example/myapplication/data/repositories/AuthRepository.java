package com.example.myapplication.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.myapplication.data.DatabaseHelper;
import com.example.myapplication.models.LoginResult;
import com.example.myapplication.models.SignupResult;
import com.example.myapplication.models.User;

public class AuthRepository {
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    public AuthRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
        this.prefs = context.getSharedPreferences("ExpenseTracker", Context.MODE_PRIVATE);
    }

    public LoginResult login(String username, String password) {
        User user = dbHelper.login(username, password);
        if (user != null) {
            saveUserSession(user);
            return new LoginResult(true, user, null);
        }
        return new LoginResult(false, null, "Invalid username or password");
    }

    public SignupResult signup(String username, String password, String pet) {
        long userId = dbHelper.signup(username, password, pet);
        if (userId > 0) {
            User user = new User((int) userId, username);
            saveUserSession(user);
            return new SignupResult(true, user, null);
        }
        if (userId == -2) {
            return new SignupResult(false, null, "Username already exists.");
        }
        
        String errorMessage;
        switch ((int) userId) {
            case -3: errorMessage = "Invalid input (empty fields)."; break;
            case -4: errorMessage = "Database access error (null)."; break;
            case -5: errorMessage = "Database table missing. Please reinstall app."; break;
            case -6: errorMessage = "Security error (hashing failed)."; break;
            case -7: errorMessage = "Failed to save user (insert error)."; break;
            case -8: errorMessage = "Database corrupted. Auto-recovery failed. Please reinstall."; break;
            case -9: errorMessage = "Database integrity check failed."; break;
            case -10: errorMessage = "Database configuration failed (PRAGMA)."; break;
            case -11: errorMessage = "Unexpected database error."; break;
            default: errorMessage = "Signup failed (Unknown error: " + userId + ")."; break;
        }
        return new SignupResult(false, null, errorMessage);
    }

    private void saveUserSession(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("userId", user.id);
        editor.putString("username", user.username);
        editor.apply();
    }

    public User getCurrentUser() {
        int userId = prefs.getInt("userId", -1);
        String username = prefs.getString("username", null);
        
        if (userId > 0) {
            
            User dbUser = dbHelper.getUser(userId);
            if (dbUser != null) {
                
                return dbUser;
            } else {

                logout();
                return null;
            }
        }
        return null;
    }

    public void ensureGuestUser() {
        int userId = prefs.getInt("userId", -1);
        if (userId > 0) return;

        int guestId = dbHelper.ensureGuestUser();
        if (guestId > 0) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("userId", guestId);
            editor.putString("username", "Guest");
            editor.apply();
        }
    }

    public void logout() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("userId");
        editor.remove("username");
        editor.apply();
    }

    public boolean updateUsername(String newUsername) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        
        boolean success = dbHelper.updateUsername(userId, newUsername);
        if (success) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", newUsername);
            editor.apply();
        }
        return success;
    }

    public boolean updatePassword(String currentPassword, String newPassword) {
        int userId = prefs.getInt("userId", -1);
        if (userId <= 0) return false;
        return dbHelper.updatePassword(userId, currentPassword, newPassword);
    }
    
    public boolean resetPassword(String username, String pet, String newPassword) {
        return dbHelper.resetPassword(username, pet, newPassword);
    }
}
