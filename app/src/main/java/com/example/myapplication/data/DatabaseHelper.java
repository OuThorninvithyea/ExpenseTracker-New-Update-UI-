package com.example.myapplication.data;

import com.example.myapplication.models.User;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 4; // Bumped to 4 for profile_picture

    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD_HASH = "password_hash";
    private static final String COL_PET_HASH = "pet_hash";
    private static final String COL_PROFILE_PICTURE = "profile_picture"; // New column

    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_EXPENSE_ID = "id";
    private static final String COL_EXPENSE_USER_ID = "user_id";
    private static final String COL_EXPENSE_CATEGORY = "category";
    private static final String COL_EXPENSE_AMOUNT = "amount";
    private static final String COL_EXPENSE_NOTE = "note";
    private static final String COL_EXPENSE_DATE = "date";

    private static final String TABLE_BUDGETS = "budgets";
    private static final String COL_BUDGET_USER_ID = "user_id";
    private static final String COL_BUDGET_CATEGORY = "category";
    private static final String COL_BUDGET_LIMIT = "limit_amount";

    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        Log.d("DatabaseHelper", "DatabaseHelper constructor called");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.d("DatabaseHelper", "Creating database tables...");
            db.execSQL("PRAGMA foreign_keys = ON");
            String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    COL_PASSWORD_HASH + " TEXT NOT NULL, " +
                    COL_PET_HASH + " TEXT NOT NULL, " +
                    COL_PROFILE_PICTURE + " TEXT)"; // Added column
            db.execSQL(createUsersTable);
            Log.d("DatabaseHelper", "Users table created");

            String createExpensesTable = "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    COL_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_EXPENSE_USER_ID + " INTEGER NOT NULL, " +
                    COL_EXPENSE_CATEGORY + " TEXT NOT NULL, " +
                    COL_EXPENSE_AMOUNT + " REAL NOT NULL, " +
                    COL_EXPENSE_NOTE + " TEXT, " +
                    COL_EXPENSE_DATE + " TEXT, " +
                    "FOREIGN KEY(" + COL_EXPENSE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
            db.execSQL(createExpensesTable);
            Log.d("DatabaseHelper", "Expenses table created");

            String createBudgetsTable = "CREATE TABLE " + TABLE_BUDGETS + " (" +
                    COL_BUDGET_USER_ID + " INTEGER NOT NULL, " +
                    COL_BUDGET_CATEGORY + " TEXT NOT NULL, " +
                    COL_BUDGET_LIMIT + " REAL NOT NULL, " +
                    "PRIMARY KEY(" + COL_BUDGET_USER_ID + ", " + COL_BUDGET_CATEGORY + "), " +
                    "FOREIGN KEY(" + COL_BUDGET_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
            db.execSQL(createBudgetsTable);
            Log.d("DatabaseHelper", "Budgets table created");
            Log.d("DatabaseHelper", "Database created successfully");
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error creating database: " + e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 4) {
            // Upgrade to version 4: Add profile_picture column
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_PROFILE_PICTURE + " TEXT");
                Log.d("DatabaseHelper", "Added profile_picture column to users table");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error adding profile_picture column: " + e.getMessage());
                // If it fails (e.g., column already exists in dev), we might ignore or recreate
            }
        } else {
            // Fallback for other versions (simple recreation for dev environment if needed, but strive for migration)
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
        }
        Log.d("DatabaseHelper", "Database upgrade completed");
    }

    public void resetDatabase(Context context) {
        Log.d("DatabaseHelper", "=== RESETTING DATABASE COMPLETELY ===");
        try {
            
            SQLiteDatabase db = null;
            try {
                db = this.getWritableDatabase();
                if (db != null && db.isOpen()) {
                    
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
                    db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
                    Log.d("DatabaseHelper", "All tables dropped");
                    db.close();
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error dropping tables: " + e.getMessage());
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error closing database: " + e.getMessage());
        }

        try {
            boolean deleted = context.deleteDatabase(DATABASE_NAME);
            Log.d("DatabaseHelper", "Database file deleted: " + deleted);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting database file: " + e.getMessage(), e);
        }

        try {
            SQLiteDatabase newDb = this.getWritableDatabase();
            if (newDb != null) {
                onCreate(newDb);
                Log.d("DatabaseHelper", "Database recreated successfully");
                newDb.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error recreating database: " + e.getMessage(), e);
        }
        
        Log.d("DatabaseHelper", "=== DATABASE RESET COMPLETED ===");
    }

    private String hashPassword(String password) {
        if (password == null) {
            Log.e("DatabaseHelper", "Hash error: password is null");
            return null;
        }
        try {
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("DatabaseHelper", "Hash error", e);
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Hash error: " + e.getMessage(), e);
            return null;
        }
    }

    public long signup(String username, String password, String pet) {
        int validationResult = validateSignupInputs(username, password, pet);
        if (validationResult != 0) return validationResult;

        SQLiteDatabase db = getWritableDatabaseWithRecovery();
        if (db == null) return -8; 

        if (!verifyTablesExist(db)) return -5; 

        String trimmedUsername = username.trim();
        String passwordHash = hashPassword(password);
        String petHash = hashPassword(pet.trim().toLowerCase());

        if (passwordHash == null || petHash == null) return -6; 

        return insertUser(db, trimmedUsername, passwordHash, petHash);
    }

    private int validateSignupInputs(String username, String password, String pet) {
        if (username == null || username.trim().isEmpty()) return -3;
        if (password == null || password.isEmpty()) return -3;
        if (pet == null || pet.trim().isEmpty()) return -3;
        return 0;
    }

    private SQLiteDatabase getWritableDatabaseWithRecovery() {
        try {
            return this.getWritableDatabase();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "DB Open failed, attempting recovery...", e);
            try {
                if (context.deleteDatabase(DATABASE_NAME)) {
                    Log.d("DatabaseHelper", "Corrupted DB deleted, retrying...");
                    return this.getWritableDatabase();
                }
            } catch (Exception retryEx) {
                Log.e("DatabaseHelper", "Recovery failed", retryEx);
            }
            return null;
        }
    }

    private boolean verifyTablesExist(SQLiteDatabase db) {
        try {
             Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
             boolean exists = cursor != null && cursor.getCount() > 0;
             if (cursor != null) cursor.close();
             return exists;
        } catch (Exception e) {
             Log.e("DatabaseHelper", "Table check failed", e);
             return false;
        }
    }

    private long insertUser(SQLiteDatabase db, String username, String passwordHash, String petHash) {
        try {
            db.execSQL("PRAGMA foreign_keys = ON");

            Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID}, COL_USERNAME + "=?", new String[]{username}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return -2; 
            }
            if (cursor != null) cursor.close();

            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, username);
            values.put(COL_PASSWORD_HASH, passwordHash);
            values.put(COL_PET_HASH, petHash);
            values.put(COL_PROFILE_PICTURE, (String) null);

            long id = db.insertOrThrow(TABLE_USERS, null, values);
            return id > 0 ? id : -7;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) return -2;
            Log.e("DatabaseHelper", "Insert failed", e);
            return -7;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Insert exception", e);
            return -11;
        }
    }

    public int ensureGuestUser() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getWritableDatabase();
            if (db == null) return -1;

            db.execSQL("PRAGMA foreign_keys = ON");

            cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=?",
                new String[]{"Guest"},
                null,
                null,
                null
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

            String passwordHash = hashPassword("guest");
            String petHash = hashPassword("guest");
            if (passwordHash == null || petHash == null) return -1;

            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, "Guest");
            values.put(COL_PASSWORD_HASH, passwordHash);
            values.put(COL_PET_HASH, petHash);
            values.put(COL_PROFILE_PICTURE, (String) null);

            long id = db.insert(TABLE_USERS, null, values);
            return id > 0 ? (int) id : -1;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureGuestUser exception: " + e.getMessage(), e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public User login(String username, String password) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            
            if (username == null || username.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Login failed: Username is null or empty");
                return null;
            }
            if (password == null || password.isEmpty()) {
                Log.e("DatabaseHelper", "Login failed: Password is null or empty");
                return null;
            }
            
            db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Login failed: Cannot get readable database");
                return null;
            }

            Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
            boolean tableExists = checkTable != null && checkTable.getCount() > 0;
            if (checkTable != null) checkTable.close();
            
            if (!tableExists) {
                Log.e("DatabaseHelper", "Login failed: Users table does not exist!");
                return null;
            }

            String trimmedUsername = username.trim();
            Log.d("DatabaseHelper", "Attempting login for user: '" + trimmedUsername + "'");
            Log.d("DatabaseHelper", "Password length: " + (password != null ? password.length() : 0));
            
            String passwordHash = hashPassword(password);
            if (passwordHash == null) {
                Log.e("DatabaseHelper", "Login failed: Hash generation failed");
                return null;
            }
            Log.d("DatabaseHelper", "Password hash generated (length: " + passwordHash.length() + ")");

            Cursor userCheck = db.query(TABLE_USERS, 
                    new String[]{COL_USER_ID, COL_USERNAME, COL_PASSWORD_HASH, COL_PROFILE_PICTURE}, 
                    COL_USERNAME + "=?", 
                    new String[]{trimmedUsername}, 
                    null, null, null);
            
            if (userCheck != null && userCheck.getCount() > 0) {
                userCheck.moveToFirst();
                int userId = userCheck.getInt(0);
                String storedUsername = userCheck.getString(1);
                String storedPasswordHash = userCheck.getString(2);
                Log.d("DatabaseHelper", "Username found! User ID: " + userId + ", Username: '" + storedUsername + "'");
                Log.d("DatabaseHelper", "Stored password hash length: " + (storedPasswordHash != null ? storedPasswordHash.length() : 0));
                Log.d("DatabaseHelper", "Input password hash length: " + passwordHash.length());

                if (storedPasswordHash != null && storedPasswordHash.equals(passwordHash)) {
                    Log.d("DatabaseHelper", "Password hash matches! Login successful.");
                    String profilePicture = null;
                    if (userCheck.getColumnIndex(COL_PROFILE_PICTURE) != -1) {
                         profilePicture = userCheck.getString(userCheck.getColumnIndexOrThrow(COL_PROFILE_PICTURE));
                    }
                    User user = new User(userId, storedUsername, profilePicture);
                    userCheck.close();
                    return user;
                } else {
                    Log.e("DatabaseHelper", "Password hash mismatch!");
                    Log.e("DatabaseHelper", "Stored hash: " + (storedPasswordHash != null ? storedPasswordHash.substring(0, Math.min(20, storedPasswordHash.length())) + "..." : "null"));
                    Log.e("DatabaseHelper", "Input hash:  " + passwordHash.substring(0, Math.min(20, passwordHash.length())) + "...");
                    userCheck.close();
                    return null;
                }
            } else {
                if (userCheck != null) userCheck.close();
                Log.e("DatabaseHelper", "Login failed: Username '" + trimmedUsername + "' does not exist in database");

                Cursor allUsers = db.query(TABLE_USERS, new String[]{COL_USERNAME}, null, null, null, null, null);
                if (allUsers != null) {
                    Log.d("DatabaseHelper", "All usernames in database:");
                    while (allUsers.moveToNext()) {
                        Log.d("DatabaseHelper", "  - '" + allUsers.getString(0) + "'");
                    }
                    allUsers.close();
                }
                return null;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Login exception: " + e.getMessage(), e);
            e.printStackTrace();
            if (cursor != null) cursor.close();
            return null;
        }
    }

    public User getUser(int id) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_USERS, 
                    new String[]{COL_USER_ID, COL_USERNAME, COL_PROFILE_PICTURE}, 
                    COL_USER_ID + "=?", 
                    new String[]{String.valueOf(id)}, 
                    null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COL_USER_ID);
                int nameIndex = cursor.getColumnIndexOrThrow(COL_USERNAME);
                int picIndex = cursor.getColumnIndex(COL_PROFILE_PICTURE);
                
                String profilePic = picIndex != -1 ? cursor.getString(picIndex) : null;
                
                User user = new User(cursor.getInt(idIndex), cursor.getString(nameIndex), profilePic);
                return user;
            }
            return null;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting user by ID: " + id, e);
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public boolean updateUsername(int userId, String newUsername) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            if (newUsername == null || newUsername.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Update username failed: New username is null or empty");
                return false;
            }
            
            String trimmedUsername = newUsername.trim();
            
            db = this.getWritableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Update username failed: Cannot get writable database");
                return false;
            }

            cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID}, 
                    COL_USERNAME + "=? AND " + COL_USER_ID + "!=?", 
                    new String[]{trimmedUsername, String.valueOf(userId)}, null, null, null);
            boolean usernameExists = cursor != null && cursor.getCount() > 0;
            if (cursor != null) cursor.close();
            
            if (usernameExists) {
                Log.e("DatabaseHelper", "Update username failed: Username '" + trimmedUsername + "' already exists");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, trimmedUsername);
            
            int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=?", 
                    new String[]{String.valueOf(userId)});
            
            if (rows > 0) {
                Log.d("DatabaseHelper", "Username updated successfully for user ID: " + userId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Update username failed: No rows affected for user ID " + userId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Update username exception: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean updatePassword(int userId, String currentPassword, String newPassword) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            if (newPassword == null || newPassword.length() < 3) {
                Log.e("DatabaseHelper", "Update password failed: New password is too short");
                return false;
            }
            
            if (currentPassword == null || currentPassword.isEmpty()) {
                Log.e("DatabaseHelper", "Update password failed: Current password is required");
                return false;
            }
            
            db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Update password failed: Cannot get readable database");
                return false;
            }

            cursor = db.query(TABLE_USERS, new String[]{COL_PASSWORD_HASH}, 
                    COL_USER_ID + "=?", new String[]{String.valueOf(userId)}, null, null, null);
            
            if (cursor == null || !cursor.moveToFirst()) {
                Log.e("DatabaseHelper", "Update password failed: User not found");
                if (cursor != null) cursor.close();
                return false;
            }
            
            String storedHash = cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD_HASH));
            cursor.close();
            
            String currentPasswordHash = hashPassword(currentPassword);
            if (currentPasswordHash == null || !currentPasswordHash.equals(storedHash)) {
                Log.e("DatabaseHelper", "Update password failed: Current password is incorrect");
                return false;
            }

            db = this.getWritableDatabase();
            String newPasswordHash = hashPassword(newPassword);
            if (newPasswordHash == null) {
                Log.e("DatabaseHelper", "Update password failed: Hash generation failed");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COL_PASSWORD_HASH, newPasswordHash);
            
            int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=?", 
                    new String[]{String.valueOf(userId)});
            
            if (rows > 0) {
                Log.d("DatabaseHelper", "Password updated successfully for user ID: " + userId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Update password failed: No rows affected for user ID " + userId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Update password exception: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean resetPassword(String username, String pet, String newPassword) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            
            String trimmedUsername = username != null ? username.trim() : "";
            if (trimmedUsername.isEmpty()) {
                Log.e("DatabaseHelper", "Reset password failed: Username is empty");
                return false;
            }
            
            String petHash = hashPassword(pet != null ? pet.toLowerCase().trim() : "");
            String newPasswordHash = hashPassword(newPassword);
            
            if (petHash == null || newPasswordHash == null) {
                Log.e("DatabaseHelper", "Reset password failed: Hash generation failed");
                return false;
            }

            cursor = db.query(TABLE_USERS,
                    new String[]{COL_USER_ID},
                    COL_USERNAME + "=? AND " + COL_PET_HASH + "=?",
                    new String[]{trimmedUsername, petHash},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put(COL_PASSWORD_HASH, newPasswordHash);
                int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=?",
                        new String[]{String.valueOf(cursor.getInt(0))});
                cursor.close();
                return rows > 0;
            }
            if (cursor != null) cursor.close();
            Log.e("DatabaseHelper", "Reset password failed: Invalid username or security answer");
            return false;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Reset password exception: " + e.getMessage(), e);
            if (cursor != null) cursor.close();
            return false;
        }
    }

    public boolean updateProfilePicture(int userId, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PROFILE_PICTURE, path);
        
        int rows = db.update(TABLE_USERS, values, COL_USER_ID + "=? ", 
                new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public long addExpense(int userId, String category, double amount, String note, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EXPENSE_USER_ID, userId);
        values.put(COL_EXPENSE_CATEGORY, category);
        values.put(COL_EXPENSE_AMOUNT, amount);
        values.put(COL_EXPENSE_NOTE, note);
        values.put(COL_EXPENSE_DATE, date);

        return db.insert(TABLE_EXPENSES, null, values);
    }

    public String getExpenses(int userId) {
        Cursor cursor = null;
        try {
            Log.d("DatabaseHelper", "getExpenses called for userId: " + userId);
            SQLiteDatabase db = this.getReadableDatabase();
            cursor = db.query(TABLE_EXPENSES,
                    new String[]{COL_EXPENSE_ID, COL_EXPENSE_CATEGORY, COL_EXPENSE_AMOUNT, COL_EXPENSE_NOTE, COL_EXPENSE_DATE},
                    COL_EXPENSE_USER_ID + "=?",
                    new String[]{String.valueOf(userId)},
                    null, null, COL_EXPENSE_ID + " DESC");

            StringBuilder json = new StringBuilder("[");
            if (cursor != null) {
                int count = 0;
                while (cursor.moveToNext()) {
                    if (json.length() > 1) json.append(",");
                    json.append("{")
                        .append("\"id\":").append(cursor.getInt(0)).append(",")
                        .append("\"category\":\"").append(cursor.getString(1) != null ? cursor.getString(1) : "").append("\",")
                        .append("\"amount\":").append(cursor.getDouble(2)).append(",")
                        .append("\"note\":\"").append(escapeJson(cursor.isNull(3) ? "" : cursor.getString(3))).append("\",")
                        .append("\"date\":\"").append(escapeJson(cursor.isNull(4) ? "" : cursor.getString(4))).append("\"")
                        .append("}");
                    count++;
                }
                Log.d("DatabaseHelper", "Loaded " + count + " expenses for user " + userId);
            }
            json.append("]");
            return json.toString();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error getting expenses for userId: " + userId, e);
            return "[]";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean updateExpense(int expenseId, String category, double amount, String note, String date) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Update expense failed: Cannot get writable database");
                return false;
            }
            
            ContentValues values = new ContentValues();
            values.put(COL_EXPENSE_CATEGORY, category);
            values.put(COL_EXPENSE_AMOUNT, amount);
            values.put(COL_EXPENSE_NOTE, note);
            values.put(COL_EXPENSE_DATE, date);

            int rows = db.update(TABLE_EXPENSES, values, COL_EXPENSE_ID + "=?",
                    new String[]{String.valueOf(expenseId)});
            
            if (rows > 0) {
                Log.d("DatabaseHelper", "Expense updated successfully: ID " + expenseId);
                return true;
            } else {
                Log.e("DatabaseHelper", "Update expense failed: No rows affected for ID " + expenseId);
                return false;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Update expense exception: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + "=?",
                new String[]{String.valueOf(expenseId)});
        return rows > 0;
    }

    public boolean clearExpenses(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        return rows >= 0;
    }

    public boolean setBudget(int userId, String category, double limit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_USER_ID, userId);
        values.put(COL_BUDGET_CATEGORY, category);
        values.put(COL_BUDGET_LIMIT, limit);

        long id = db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        return id > 0;
    }

    public String getBudgets(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BUDGETS,
                new String[]{COL_BUDGET_CATEGORY, COL_BUDGET_LIMIT},
                COL_BUDGET_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, null);

        StringBuilder json = new StringBuilder("[");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (json.length() > 1) json.append(",");
                json.append("{")
                    .append("\"category\":\"").append(cursor.getString(0)).append("\",")
                    .append("\"limit\":").append(cursor.getDouble(1))
                    .append("}");
            }
            cursor.close();
        }
        json.append("]");
        return json.toString();
    }

    public boolean deleteBudget(int userId, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_BUDGETS,
                COL_BUDGET_USER_ID + "=? AND " + COL_BUDGET_CATEGORY + "=?",
                new String[]{String.valueOf(userId), category});
        return rows > 0;
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public boolean verifyDatabase() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Database verification failed: Cannot get database");
                return false;
            }

            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
                boolean exists = cursor != null && cursor.getCount() > 0;
                
                if (exists) {
                    
                    Cursor userCount = null;
                    try {
                        userCount = db.query(TABLE_USERS, new String[]{"COUNT(*) as count"}, null, null, null, null, null);
                        int count = 0;
                        if (userCount != null && userCount.moveToFirst()) {
                            count = userCount.getInt(0);
                        }
                        Log.d("DatabaseHelper", "Database verified: Users table exists with " + count + " users");
                        return true;
                    } finally {
                        if (userCount != null) userCount.close();
                    }
                } else {
                    Log.e("DatabaseHelper", "Database verification failed: Users table does not exist");
                    return false;
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Database verification exception: " + e.getMessage(), e);
            return false;
        }
    }

}
