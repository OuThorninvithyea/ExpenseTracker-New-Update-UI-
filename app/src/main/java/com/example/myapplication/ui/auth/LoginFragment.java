package com.example.myapplication.ui.auth;

import com.example.myapplication.R;
import com.example.myapplication.data.repositories.AuthRepository;
import com.example.myapplication.models.LoginResult;
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

public class LoginFragment extends Fragment {
    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin, btnSignup, btnForgotPassword, btnGuest;
    private TextView tvError;
    private AuthRepository authRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());

        etUsername = view.findViewById(R.id.etUsername);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnSignup = view.findViewById(R.id.btnSignup);
        btnForgotPassword = view.findViewById(R.id.btnForgotPassword);
        btnGuest = view.findViewById(R.id.btnGuest);
        tvError = view.findViewById(R.id.tvError);

        btnLogin.setOnClickListener(v -> attemptLogin());

        btnGuest.setOnClickListener(v -> {
            authRepository.ensureGuestUser();
            navigateToHome();
        });

        btnSignup.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });

        btnForgotPassword.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ForgotPasswordFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });
    }

    private void navigateToHome() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new HomeFragment())
                .commit();
            
            mainActivity.bottomNavigation.setVisibility(View.VISIBLE);
            mainActivity.bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void attemptLogin() {
        
        tvError.setVisibility(View.GONE);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(username)) {
            tvError.setText("Please enter a username");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tvError.setText("Please enter a password");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        LoginResult result = authRepository.login(username, password);

        if (result.success) {
            Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof com.example.myapplication.ui.main.MainActivity) {
                ((com.example.myapplication.ui.main.MainActivity) getActivity()).navigateToHome();
            }
        } else {
            
            tvError.setText(result.error);
            tvError.setVisibility(View.VISIBLE);
            etPassword.setText(""); 
        }
    }
}
