package com.example.myapplication;

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

/**
 * DatabaseHelper - Manages SQLite database operations for the Expense Tracker app.
 * 
 * This class extends SQLiteOpenHelper and handles:
 * - Database creation and version management
 * - User authentication (signup, login, password management)
 * - Expense CRUD operations (Create, Read, Update, Delete)
 * - Budget management (set, get, delete budgets)
 * - Password hashing using SHA-256 for security
 * 
 * Database Schema:
 * - users: Stores user accounts with hashed passwords
 * - expenses: Stores expense records linked to users
 * - budgets: Stores budget limits per category per user
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Database name and version
    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 3; // Incremented to force complete database reset

    // Users table - stores user account information
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";                    // Primary key, auto-increment
    private static final String COL_USERNAME = "username";              // Unique username
    private static final String COL_PASSWORD_HASH = "password_hash";    // SHA-256 hashed password
    private static final String COL_PET_HASH = "pet_hash";             // Security question answer hash

    // Expenses table - stores expense transactions
    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_EXPENSE_ID = "id";                  // Primary key, auto-increment
    private static final String COL_EXPENSE_USER_ID = "user_id";       // Foreign key to users table
    private static final String COL_EXPENSE_CATEGORY = "category";      // Expense category (Food, Transport, etc.)
    private static final String COL_EXPENSE_AMOUNT = "amount";          // Expense amount (REAL/float)
    private static final String COL_EXPENSE_NOTE = "note";              // Optional note/description
    private static final String COL_EXPENSE_DATE = "date";              // Date of expense

    // Budgets table - stores budget limits per category
    private static final String TABLE_BUDGETS = "budgets";
    private static final String COL_BUDGET_USER_ID = "user_id";        // Foreign key to users table
    private static final String COL_BUDGET_CATEGORY = "category";       // Category name
    private static final String COL_BUDGET_LIMIT = "limit_amount";    // Budget limit amount

    private Context context; // Application context for database operations
    
    /**
     * Constructor - Creates a DatabaseHelper instance.
     * 
     * @param context Application context needed for database operations
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        Log.d("DatabaseHelper", "DatabaseHelper constructor called");
    }

    /**
     * Called when the database is created for the first time.
     * Creates all necessary tables: users, expenses, and budgets.
     * 
     * @param db The database instance
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            Log.d("DatabaseHelper", "Creating database tables...");
            
            // Enable foreign key constraints to maintain referential integrity
            // This ensures that expenses and budgets must reference valid users
            db.execSQL("PRAGMA foreign_keys = ON");
            
            // Create users table - stores user account information
            // Columns: id (primary key), username (unique), password_hash, pet_hash
            String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    COL_PASSWORD_HASH + " TEXT NOT NULL, " +
                    COL_PET_HASH + " TEXT NOT NULL)";
            db.execSQL(createUsersTable);
            Log.d("DatabaseHelper", "Users table created");

            // Create expenses table - stores expense transactions
            // Columns: id (primary key), user_id (foreign key), category, amount, note, date
            // Foreign key ensures expenses belong to valid users
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

            // Create budgets table - stores budget limits per category per user
            // Composite primary key (user_id, category) ensures one budget per category per user
            // Foreign key ensures budgets belong to valid users
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
    
    /**
     * Called every time the database is opened.
     * Ensures foreign key constraints are enabled for data integrity.
     * 
     * @param db The database instance
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign keys every time database is opened
        // SQLite requires this to be set per connection
        db.execSQL("PRAGMA foreign_keys = ON");
    }

    /**
     * Called when the database version is upgraded.
     * Currently drops all tables and recreates them (simple migration strategy).
     * 
     * @param db The database instance
     * @param oldVersion Previous database version
     * @param newVersion New database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);
        // Drop all tables and recreate
        // Note: This will delete all existing data. For production apps, consider
        // implementing proper migration scripts to preserve user data.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
        Log.d("DatabaseHelper", "Database upgrade completed");
    }
    
    /**
     * Completely resets the database by deleting all tables and the database file itself.
     * Used for testing or when user wants to start fresh.
     * 
     * @param context Application context needed to delete database file
     */
    public void resetDatabase(Context context) {
        Log.d("DatabaseHelper", "=== RESETTING DATABASE COMPLETELY ===");
        try {
            // Close any open database connections first
            SQLiteDatabase db = null;
            try {
                db = this.getWritableDatabase();
                if (db != null && db.isOpen()) {
                    // Drop all tables
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
        
        // Delete the database file completely
        try {
            boolean deleted = context.deleteDatabase(DATABASE_NAME);
            Log.d("DatabaseHelper", "Database file deleted: " + deleted);
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting database file: " + e.getMessage(), e);
        }
        
        // Recreate database by getting a new instance
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

    /**
     * Hashes a password using SHA-256 algorithm.
     * Passwords are never stored in plain text - only their hash is stored.
     * 
     * @param password The plain text password to hash
     * @return The hexadecimal string representation of the hash, or null if error
     */
    private String hashPassword(String password) {
        if (password == null) {
            Log.e("DatabaseHelper", "Hash error: password is null");
            return null;
        }
        try {
            // Get SHA-256 message digest instance
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Convert password to bytes using UTF-8 encoding
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                // Pad single-digit hex values with leading zero
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

    /**
     * Creates a new user account in the database.
     * Validates inputs, checks for duplicate usernames, and stores hashed credentials.
     * 
     * @param username The desired username (must be unique)
     * @param password The user's password (will be hashed before storage)
     * @param pet The security question answer (will be hashed before storage)
     * @return The new user's ID if successful, -1 for errors, -2 if username already exists
     */
    public long signup(String username, String password, String pet) {
        SQLiteDatabase db = null;
        try {
            // Validate inputs
            if (username == null || username.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Signup failed: Username is null or empty");
                return -1;
            }
            if (password == null || password.isEmpty()) {
                Log.e("DatabaseHelper", "Signup failed: Password is null or empty");
                return -1;
            }
            if (pet == null || pet.trim().isEmpty()) {
                Log.e("DatabaseHelper", "Signup failed: Pet/security answer is null or empty");
                return -1;
            }
            
            db = this.getWritableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Signup failed: Cannot get writable database");
                return -1;
            }
            
            // Verify table exists - SQLiteOpenHelper should have created it, but check anyway
            Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
            boolean tableExists = checkTable != null && checkTable.getCount() > 0;
            if (checkTable != null) checkTable.close();
            
            if (!tableExists) {
                Log.e("DatabaseHelper", "Signup failed: Users table does not exist! Database may be corrupted.");
                Log.e("DatabaseHelper", "Please uninstall and reinstall the app to recreate the database.");
                return -1;
            }
            
            // Trim username for consistency
            String trimmedUsername = username.trim();
            String trimmedPet = pet.trim().toLowerCase();
            
            Log.d("DatabaseHelper", "Signup attempt for username: " + trimmedUsername);
            
            String passwordHash = hashPassword(password);
            String petHash = hashPassword(trimmedPet);
            
            if (passwordHash == null || petHash == null) {
                Log.e("DatabaseHelper", "Signup failed: Hash generation failed - passwordHash: " + (passwordHash != null) + ", petHash: " + (petHash != null));
                return -1;
            }

            // Enable foreign keys for this connection
            db.execSQL("PRAGMA foreign_keys = ON");
            
            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, trimmedUsername);
            values.put(COL_PASSWORD_HASH, passwordHash);
            values.put(COL_PET_HASH, petHash);

            Log.d("DatabaseHelper", "Attempting to insert user: " + trimmedUsername);
            
            // First check if username already exists
            Cursor checkUser = db.query(TABLE_USERS, new String[]{COL_USER_ID}, COL_USERNAME + "=?", new String[]{trimmedUsername}, null, null, null);
            boolean usernameExists = checkUser != null && checkUser.getCount() > 0;
            if (checkUser != null) checkUser.close();
            
            if (usernameExists) {
                Log.e("DatabaseHelper", "Signup failed: Username '" + trimmedUsername + "' already exists");
                return -2; // Return -2 to indicate username exists (different from -1 for other errors)
            }
            
            long id = -1;
            try {
                id = db.insertOrThrow(TABLE_USERS, null, values);
                Log.d("DatabaseHelper", "Signup successful for user: " + trimmedUsername + " with ID: " + id);
            } catch (SQLException e) {
                Log.e("DatabaseHelper", "SQLException during insert: " + e.getMessage(), e);
                // Check if it's a unique constraint violation (username already exists)
                if (e.getMessage() != null && (e.getMessage().contains("UNIQUE constraint") || e.getMessage().contains("unique"))) {
                    Log.e("DatabaseHelper", "Username '" + trimmedUsername + "' already exists (caught in exception)");
                    id = -2; // Username exists
                } else {
                    Log.e("DatabaseHelper", "Database insert failed: " + e.getMessage());
                    id = -1; // Other database error
                }
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Unexpected exception during insert: " + e.getMessage(), e);
                id = -1; // Database error
            }
            
            return id;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Signup exception: " + e.getMessage(), e);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Ensures a single default "Guest" user exists and returns its user id.
     * This is used when the app runs without login/signup screens.
     * If Guest user doesn't exist, creates one with default credentials.
     * 
     * @return The Guest user's ID, or -1 if creation failed
     */
    public int ensureGuestUser() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getWritableDatabase();
            if (db == null) return -1;

            db.execSQL("PRAGMA foreign_keys = ON");

            // Check if guest already exists
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

            // Create guest
            String passwordHash = hashPassword("guest");
            String petHash = hashPassword("guest");
            if (passwordHash == null || petHash == null) return -1;

            ContentValues values = new ContentValues();
            values.put(COL_USERNAME, "Guest");
            values.put(COL_PASSWORD_HASH, passwordHash);
            values.put(COL_PET_HASH, petHash);

            long id = db.insert(TABLE_USERS, null, values);
            return id > 0 ? (int) id : -1;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "ensureGuestUser exception: " + e.getMessage(), e);
            return -1;
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    /**
     * Authenticates a user by verifying username and password.
     * Compares the provided password hash with the stored hash.
     * 
     * @param username The username to authenticate
     * @param password The password to verify
     * @return User object if authentication succeeds, null otherwise
     */
    public User login(String username, String password) {
        Cursor cursor = null;
        SQLiteDatabase db = null;
        try {
            // Validate inputs
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
            
            // Verify table exists
            Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
            boolean tableExists = checkTable != null && checkTable.getCount() > 0;
            if (checkTable != null) checkTable.close();
            
            if (!tableExists) {
                Log.e("DatabaseHelper", "Login failed: Users table does not exist!");
                return null;
            }
            
            // Trim username to match signup behavior
            String trimmedUsername = username.trim();
            Log.d("DatabaseHelper", "Attempting login for user: '" + trimmedUsername + "'");
            Log.d("DatabaseHelper", "Password length: " + (password != null ? password.length() : 0));
            
            String passwordHash = hashPassword(password);
            if (passwordHash == null) {
                Log.e("DatabaseHelper", "Login failed: Hash generation failed");
                return null;
            }
            Log.d("DatabaseHelper", "Password hash generated (length: " + passwordHash.length() + ")");
            
            // First check if username exists
            Cursor userCheck = db.query(TABLE_USERS, 
                    new String[]{COL_USER_ID, COL_USERNAME, COL_PASSWORD_HASH}, 
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
                
                // Compare hashes
                if (storedPasswordHash != null && storedPasswordHash.equals(passwordHash)) {
                    Log.d("DatabaseHelper", "Password hash matches! Login successful.");
                    User user = new User(userId, storedUsername);
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
                
                // Debug: List all usernames in database
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

    /**
     * Updates a user's username.
     * Validates that the new username is not already taken by another user.
     * 
     * @param userId The ID of the user to update
     * @param newUsername The new username (must be unique)
     * @return true if update successful, false otherwise
     */
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
            
            // Check if new username already exists (excluding current user)
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
    
    /**
     * Updates a user's password.
     * Requires the current password to be verified before allowing the change.
     * 
     * @param userId The ID of the user
     * @param currentPassword The user's current password (for verification)
     * @param newPassword The new password (must be at least 3 characters)
     * @return true if update successful, false otherwise
     */
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
            
            // Verify current password
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
            
            // Update password
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

    /**
     * Resets a user's password using security question answer (pet).
     * Used for password recovery when user forgets their password.
     * 
     * @param username The username of the account
     * @param pet The security question answer
     * @param newPassword The new password to set
     * @return true if reset successful, false if username or pet answer incorrect
     */
    public boolean resetPassword(String username, String pet, String newPassword) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // Trim username for consistency
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

    /**
     * Adds a new expense record to the database.
     * 
     * @param userId The ID of the user who owns this expense
     * @param category The expense category (e.g., "Food", "Transport")
     * @param amount The expense amount
     * @param note Optional note/description
     * @param date The date of the expense
     * @return The ID of the newly created expense, or -1 if failed
     */
    public long addExpense(int userId, String category, double amount, String note, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_EXPENSE_USER_ID, userId);
        values.put(COL_EXPENSE_CATEGORY, category);
        values.put(COL_EXPENSE_AMOUNT, amount);
        values.put(COL_EXPENSE_NOTE, note);
        values.put(COL_EXPENSE_DATE, date);

        long id = db.insert(TABLE_EXPENSES, null, values);
        db.close();
        return id;
    }

    /**
     * Retrieves all expenses for a specific user as a JSON string.
     * Expenses are ordered by ID descending (newest first).
     * 
     * @param userId The ID of the user
     * @return JSON array string containing all expenses, empty array if none found
     */
    public String getExpenses(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES,
                new String[]{COL_EXPENSE_ID, COL_EXPENSE_CATEGORY, COL_EXPENSE_AMOUNT, COL_EXPENSE_NOTE, COL_EXPENSE_DATE},
                COL_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)},
                null, null, COL_EXPENSE_ID + " DESC");

        StringBuilder json = new StringBuilder("[");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (json.length() > 1) json.append(",");
                json.append("{")
                    .append("\"id\":").append(cursor.getInt(0)).append(",")
                    .append("\"category\":\"").append(cursor.getString(1) != null ? cursor.getString(1) : "").append("\",")
                    .append("\"amount\":").append(cursor.getDouble(2)).append(",")
                    .append("\"note\":\"").append(escapeJson(cursor.isNull(3) ? "" : cursor.getString(3))).append("\",")
                    .append("\"date\":\"").append(escapeJson(cursor.isNull(4) ? "" : cursor.getString(4))).append("\"")
                    .append("}");
            }
            cursor.close();
        }
        json.append("]");
        db.close();
        return json.toString();
    }

    /**
     * Updates an existing expense record.
     * 
     * @param expenseId The ID of the expense to update
     * @param category The new category
     * @param amount The new amount
     * @param note The new note
     * @param date The new date
     * @return true if update successful, false otherwise
     */
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

    /**
     * Deletes an expense record from the database.
     * 
     * @param expenseId The ID of the expense to delete
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteExpense(int expenseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_ID + "=?",
                new String[]{String.valueOf(expenseId)});
        db.close();
        return rows > 0;
    }

    /**
     * Deletes all expenses for a specific user.
     * 
     * @param userId The ID of the user
     * @return true if operation successful (even if no expenses existed)
     */
    public boolean clearExpenses(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_EXPENSES, COL_EXPENSE_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        db.close();
        return rows >= 0;
    }

    /**
     * Sets or updates a budget limit for a category.
     * Uses INSERT OR REPLACE to update if budget already exists for that category.
     * 
     * @param userId The ID of the user
     * @param category The category name
     * @param limit The budget limit amount
     * @return true if operation successful, false otherwise
     */
    public boolean setBudget(int userId, String category, double limit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_BUDGET_USER_ID, userId);
        values.put(COL_BUDGET_CATEGORY, category);
        values.put(COL_BUDGET_LIMIT, limit);

        long id = db.insertWithOnConflict(TABLE_BUDGETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id > 0;
    }

    /**
     * Retrieves all budgets for a specific user as a JSON string.
     * 
     * @param userId The ID of the user
     * @return JSON array string containing all budgets, empty array if none found
     */
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
        db.close();
        return json.toString();
    }

    /**
     * Deletes a budget for a specific category.
     * 
     * @param userId The ID of the user
     * @param category The category name
     * @return true if deletion successful, false otherwise
     */
    public boolean deleteBudget(int userId, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_BUDGETS,
                COL_BUDGET_USER_ID + "=? AND " + COL_BUDGET_CATEGORY + "=?",
                new String[]{String.valueOf(userId), category});
        db.close();
        return rows > 0;
    }

    /**
     * Escapes special characters in a string for safe JSON encoding.
     * Prevents JSON injection attacks and ensures valid JSON format.
     * 
     * @param str The string to escape
     * @return Escaped string safe for JSON, empty string if input is null
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Debug method to verify database state.
     * Checks if the users table exists and counts the number of users.
     * 
     * @return true if database is valid, false otherwise
     */
    public boolean verifyDatabase() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            if (db == null) {
                Log.e("DatabaseHelper", "Database verification failed: Cannot get database");
                return false;
            }
            
            // Check if users table exists
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{TABLE_USERS});
                boolean exists = cursor != null && cursor.getCount() > 0;
                
                if (exists) {
                    // Count users
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

    /**
     * Inner class representing a User entity.
     * Used to return user information from login and other operations.
     */
    public static class User {
        public int id;           // User's unique ID
        public String username;  // User's username

        /**
         * Constructor for User object.
         * 
         * @param id The user's unique ID
         * @param username The user's username
         */
        public User(int id, String username) {
            this.id = id;
            this.username = username;
        }
    }
}
