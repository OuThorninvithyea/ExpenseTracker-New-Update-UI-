package com.example.myapplication.ui.main;

import com.example.myapplication.R;
import com.example.myapplication.data.repositories.AuthRepository;
import com.example.myapplication.models.User;
import com.example.myapplication.ui.auth.LoginFragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    
    public BottomNavigationView bottomNavigation;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        SharedPreferences prefs = getSharedPreferences("AppSettings", 0);
        int darkMode = prefs.getInt("dark_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(darkMode);

        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);

            authRepository = new AuthRepository(this);
            bottomNavigation = findViewById(R.id.bottomNavigation);
            
            if (bottomNavigation == null) {
                throw new RuntimeException("Bottom navigation view not found in layout");
            }
            
            User currentUser = authRepository.getCurrentUser();
            boolean isLoggedIn = currentUser != null;
            
            if (!isLoggedIn) {
                if (savedInstanceState == null) {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new LoginFragment())
                        .commit();
                }
                bottomNavigation.setVisibility(View.GONE);
            } else {
                bottomNavigation.setVisibility(View.VISIBLE);
            }

            View fragmentContainer = findViewById(R.id.fragmentContainer);
            if (fragmentContainer != null) {
                ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, insets) -> {
                    int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(0, top, 0, 0);
                    return insets;
                });
                ViewCompat.requestApplyInsets(fragmentContainer);
            }
            
            bottomNavigation.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                
                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_analytics) {
                    selectedFragment = new AnalyticsFragment();
                } else if (itemId == R.id.nav_budget) {
                    selectedFragment = new BudgetFragment();
                } else if (itemId == R.id.nav_settings) {
                    selectedFragment = new SettingsFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
                    return true;
                }
                return false;
            });

            if (savedInstanceState == null && isLoggedIn) {
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error during onCreate", e);
            android.widget.Toast.makeText(this, "Error starting app: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    public void showLoginScreen() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, new LoginFragment())
            .commit();
        bottomNavigation.setVisibility(View.GONE);
    }

    public void navigateToHome() {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragmentContainer, new HomeFragment())
            .commit();
        bottomNavigation.setVisibility(View.VISIBLE);
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }
}