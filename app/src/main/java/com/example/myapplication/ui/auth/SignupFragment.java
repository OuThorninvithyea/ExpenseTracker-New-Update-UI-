    package com.example.myapplication.ui.auth;

import com.example.myapplication.R;
import com.example.myapplication.data.repositories.AuthRepository;
import com.example.myapplication.models.SignupResult;
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

public class SignupFragment extends Fragment {
    private TextInputEditText etUsername, etPassword, etPet;
    private MaterialButton btnSignup, btnBackToLogin;
    private TextView tvError;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        etPet = view.findViewById(R.id.etPet);
        btnSignup = view.findViewById(R.id.btnSignup);
        btnBackToLogin = view.findViewById(R.id.btnBackToLogin);
        tvError = view.findViewById(R.id.tvError);

        btnSignup.setOnClickListener(v -> attemptSignup());

        btnBackToLogin.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void attemptSignup() {
        
        tvError.setVisibility(View.GONE);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String pet = etPet.getText() != null ? etPet.getText().toString().trim() : "";
        
        android.util.Log.d("SignupDebug", "Attempting signup. Username len: " + username.length() + ", Password len: " + password.length());

        if (TextUtils.isEmpty(username)) {
            tvError.setText("Please enter a username");
            tvError.setVisibility(View.VISIBLE);
            etUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 3) {
            tvError.setText("Password must be at least 3 characters (Current: " + password.length() + ")");
            tvError.setVisibility(View.VISIBLE);
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pet)) {
            tvError.setText("Please answer the security question");
            tvError.setVisibility(View.VISIBLE);
            etPet.requestFocus();
            return;
        }

        SignupResult result = authRepository.signup(username, password, pet);

        if (result.success) {
            Toast.makeText(getContext(), "Signup successful!", Toast.LENGTH_SHORT).show();
            
            if (getActivity() instanceof com.example.myapplication.ui.main.MainActivity) {
                ((com.example.myapplication.ui.main.MainActivity) getActivity()).navigateToHome();
            }
        } else {
            if (result.error != null) {
                tvError.setText(result.error);
            } else {
                tvError.setText("Signup failed. Please try again.");
            }
            tvError.setVisibility(View.VISIBLE);
        }
    }
}
