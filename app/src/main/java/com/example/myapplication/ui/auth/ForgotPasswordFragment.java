package com.example.myapplication.ui.auth;

import com.example.myapplication.R;
import com.example.myapplication.data.repositories.AuthRepository;
import com.example.myapplication.ui.main.HomeFragment;
import com.example.myapplication.ui.main.MainActivity;

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

public class ForgotPasswordFragment extends Fragment {
    private TextInputEditText etUsername, etPet, etNewPassword;
    private MaterialButton btnResetPassword, btnBackToLogin;
    private TextView tvError;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());

        etUsername = view.findViewById(R.id.etUsername);
        etPet = view.findViewById(R.id.etPet);
        etNewPassword = view.findViewById(R.id.etNewPassword);
        btnResetPassword = view.findViewById(R.id.btnResetPassword);
        btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        tvError = view.findViewById(R.id.tvError);

        btnResetPassword.setOnClickListener(v -> attemptResetPassword());

        btnBackToLogin.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void attemptResetPassword() {
        
        tvError.setVisibility(View.GONE);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String pet = etPet.getText() != null ? etPet.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";

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

        boolean success = authRepository.resetPassword(username, pet, newPassword);

        if (success) {
            
            Toast.makeText(requireContext(), "Password reset successfully! Please sign in with your new password.", Toast.LENGTH_LONG).show();

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .commit();
            }
        } else {
            
            tvError.setText("Invalid username or security answer. Please try again.");
            tvError.setVisibility(View.VISIBLE);
            etNewPassword.setText(""); 
        }
    }
}
