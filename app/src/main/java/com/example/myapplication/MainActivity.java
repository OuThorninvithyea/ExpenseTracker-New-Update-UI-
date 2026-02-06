package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * MainActivity - The main entry point of the Expense Tracker application.
 * 
 * This activity serves as the container for all fragments and manages:
 * - User authentication check - shows login screen if not logged in
 * - Bottom navigation between different screens (Home, Analytics, Budget, Settings)
 * - Dark mode theme preference loading
 * - System window insets handling for proper layout on devices with notches
 * - Fragment transactions and navigation
 */
public class MainActivity extends AppCompatActivity {
    // Bottom navigation view that allows switching between main screens
    public BottomNavigationView bottomNavigation;
    
    // DataManager singleton instance for accessing database and user data
    private DataManager dataManager;

    /**
     * Called when the activity is first created.
     * Sets up the UI, loads preferences, initializes navigation, and displays the default fragment.
     * 
     * @param savedInstanceState Previously saved instance state (null on first launch)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load dark mode preference from SharedPreferences
        // This ensures the app remembers the user's theme choice across app restarts
        SharedPreferences prefs = getSharedPreferences("AppSettings", 0);
        int darkMode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);
        
        // Set the main layout that contains the fragment container and bottom navigation
        setContentView(R.layout.activity_main);

        // Initialize DataManager singleton - this handles all database operations
        dataManager = DataManager.getInstance(this);
        
        // Initialize bottom navigation view
        bottomNavigation = findViewById(R.id.bottomNavigation);
        
        // Check if user is logged in (not Guest)
        DatabaseHelper.User currentUser = dataManager.getCurrentUser();
        boolean isLoggedIn = currentUser != null && !currentUser.username.equals("Guest");
        
        if (!isLoggedIn) {
            // No user logged in or Guest user - show login screen
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new LoginFragment())
                .commit();
            // Hide bottom navigation until user logs in
            bottomNavigation.setVisibility(View.GONE);
        } else {
            // User is logged in - show main app
            bottomNavigation.setVisibility(View.VISIBLE);
        }

        // Apply system bar (status bar/notch) inset padding to all fragment screens
        // This ensures content doesn't get hidden behind the status bar or device notch
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragmentContainer), (v, insets) -> {
            // Get the top inset (height of status bar/notch area)
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            // Add padding to the top to push content below the system bars
            v.setPadding(0, top, 0, 0);
            return insets;
        });
        // Request that window insets be applied
        ViewCompat.requestApplyInsets(findViewById(R.id.fragmentContainer));
        
        // Set up listener for bottom navigation item selection
        // When user taps a navigation item, replace the current fragment with the selected one
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            // Determine which fragment to show based on selected navigation item
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_analytics) {
                selectedFragment = new AnalyticsFragment();
            } else if (itemId == R.id.nav_budget) {
                selectedFragment = new BudgetFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            // If a fragment was selected, replace the current fragment in the container
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, selectedFragment)
                    .commit();
                return true; // Indicate the selection was handled
            }
            return false; // Selection was not handled
        });

        // Load default fragment (Home) only on first launch and if user is logged in
        // If savedInstanceState is not null, the activity is being recreated (e.g., after rotation)
        // and we should preserve the current fragment state
        if (savedInstanceState == null && isLoggedIn) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }
    
    /**
     * Shows the login screen and hides bottom navigation.
     * Called when user logs out.
     */
    public void showLoginScreen() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, new LoginFragment())
            .commit();
        bottomNavigation.setVisibility(View.GONE);
    }
}