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
 * ForgotPasswordFragment - Handles password reset functionality.
 * 
 * Features:
 * - Username and security question answer input
 * - New password input
 * - Password reset validation
 * - Navigation back to login screen
 * - Error message display
 */
public class ForgotPasswordFragment extends Fragment {
    private TextInputEditText etUsername, etPet, etNewPassword;
    private MaterialButton btnResetPassword, btnBackToLogin;
    private TextView tvError;
    private DataManager dataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize DataManager
        dataManager = DataManager.getInstance(requireContext());

        // Initialize UI components
        etUsername = view.findViewById(R.id.etUsername);
        etPet = view.findViewById(R.id.etPet);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        tvError = view.findViewById(R.id.tvError);

        // Setup reset password button click listener
        btnResetPassword.setOnClickListener(v -> attemptResetPassword());

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
     * Attempts to reset the user's password.
     * Validates inputs and calls DataManager resetPassword method.
     */
    private void attemptResetPassword() {
        // Hide previous error messages
        tvError.setVisibility(View.GONE);

        // Get input values
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String pet = etPet.getText() != null ? etPet.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";

        // Validate inputs
        if (TextUtils.isEmpty(username)) {
            tvError.setText("Please enter your username");
            tvError.setVisibility(View.VISIBLE);
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pet)) {
            tvError.setText("Please enter your security question answer");
            tvError.setVisibility(View.VISIBLE);
            etPet.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 3) {
            tvError.setText("New password must be at least 3 characters");
            tvError.setVisibility(View.VISIBLE);
            etNewPassword.requestFocus();
            return;
        }

        // Attempt password reset using DataManager
        boolean success = dataManager.resetPassword(username, pet, newPassword);

        if (success) {
            // Password reset successful - show success message and navigate to login
            Toast.makeText(requireContext(), "Password reset successfully! Please sign in with your new password.", Toast.LENGTH_LONG).show();
            
            // Navigate back to login screen
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .commit();
            }
        } else {
            // Password reset failed - show error message
            tvError.setText("Invalid username or security answer. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            etNewPassword.setText(""); // Clear password field
        }
    }
}
