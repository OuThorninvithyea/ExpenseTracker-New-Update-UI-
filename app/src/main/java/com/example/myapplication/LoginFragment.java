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
 * LoginFragment - Handles user authentication/login.
 * 
 * Features:
 * - Username and password input
 * - Login validation
 * - Navigation to signup screen
 * - Forgot password navigation
 * - Error message display
 */
public class LoginFragment extends Fragment {
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin, btnSignup, btnForgotPassword;
    private TextView tvError;
    private DataManager dataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize DataManager
        dataManager = DataManager.getInstance(requireContext());

        // Initialize UI components
        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnSignup = view.findViewById(R.id.btnSignup);
        btnForgotPassword = view.findViewById(R.id.btnForgotPassword);
        tvError = view.findViewById(R.id.tvError);

        // Setup login button click listener
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Setup signup button - navigate to signup screen
        btnSignup.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });

        // Setup forgot password button - navigate to forgot password screen
        btnForgotPassword.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ForgotPasswordFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    /**
     * Attempts to log in the user with provided credentials.
     * Validates inputs and calls DataManager login method.
     */
    private void attemptLogin() {
        // Hide previous error messages
        tvError.setVisibility(View.GONE);

        // Get input values
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Validate inputs
        if (TextUtils.isEmpty(username)) {
            tvError.setText("Please enter your username");
            tvError.setVisibility(View.VISIBLE);
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tvError.setText("Please enter your password");
            tvError.setVisibility(View.VISIBLE);
            etPassword.requestFocus();
            return;
        }

        // Attempt login using DataManager
        DataManager.LoginResult result = dataManager.login(username, password);

        if (result.success) {
            // Login successful - navigate to main app
            Toast.makeText(requireContext(), "Welcome back, " + result.user.username + "!", Toast.LENGTH_SHORT).show();
            
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
            // Login failed - show error message
            tvError.setText(result.error != null ? result.error : "Login failed. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            etPassword.setText(""); // Clear password field
        }
    }
}
