package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * SignupFragment - Handles new user registration.
 * 
 * Features:
 * - Username, password, and security question input
 * - Input validation
 * - Account creation
 * - Navigation back to login screen
 * - Error message display
 */
public class SignupFragment extends Fragment {
    private TextInputEditText etUsername, etPassword, etPet;
    private MaterialButton btnSignup, btnBackToLogin;
    private TextView tvError;
    private DataManager dataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize DataManager
        dataManager = DataManager.getInstance(requireContext());

        // Initialize UI components
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        etPet = view.findViewById(R.id.etPet);
        btnSignup = view.findViewById(R.id.btnSignup);
        btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        tvError = view.findViewById(R.id.tvError);

        // Setup signup button click listener
        btnSignup.setOnClickListener(v -> attemptSignup());

        // Setup back to login button - navigate back to login screen
        btnBackToLogin.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    /**
     * Attempts to create a new user account.
     * Validates inputs and calls DataManager signup method.
     */
    private void attemptSignup() {
        // Hide previous error messages
        tvError.setVisibility(View.GONE);

        // Get input values
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String pet = etPet.getText() != null ? etPet.getText().toString().trim() : "";

        // Validate inputs
        if (TextUtils.isEmpty(username)) {
            tvError.setText("Please enter a username");
            tvError.setVisibility(View.VISIBLE);
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 3) {
            tvError.setText("Password must be at least 3 characters");
            tvError.setVisibility(View.VISIBLE);
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pet)) {
            tvError.setText("Please enter a security question answer");
            tvError.setVisibility(View.VISIBLE);
            etPet.requestFocus();
            return;
        }

        // Attempt signup using DataManager
        DataManager.SignupResult result = dataManager.signup(username, password, pet);

        if (result.success) {
            // Signup successful - show success message and navigate to main app
            Toast.makeText(requireContext(), "Account created successfully! Welcome, " + result.user.username + "!", Toast.LENGTH_SHORT).show();
            
            // Navigate to HomeFragment
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
                // Show bottom navigation
                mainActivity.bottomNavigation.setVisibility(View.VISIBLE);
                mainActivity.bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        } else {
            // Signup failed - show error message
            tvError.setText(result.error != null ? result.error : "Signup failed. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            etPassword.setText(""); // Clear password field
        }
    }
}
