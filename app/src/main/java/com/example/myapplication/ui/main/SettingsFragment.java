package com.example.myapplication.ui.main;

import com.example.myapplication.R;
import com.example.myapplication.R;
import com.example.myapplication.data.repositories.AuthRepository;
import com.example.myapplication.services.ExpenseService;
import com.example.myapplication.models.User;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsFragment extends Fragment {
    private TextView tvUsername, tvUserInitial;
    private MaterialButton btnLogout, btnClearData, btnEditProfile;
    private SwitchMaterial switchDarkMode;
    private AuthRepository authRepository;
    private ExpenseService expenseService;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());
        expenseService = new ExpenseService(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        
        tvUsername = view.findViewById(R.id.tvUsername);
        tvUserInitial = view.findViewById(R.id.tvUserInitial);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        btnClearData = view.findViewById(R.id.btnClearData);

        loadDarkModeState();

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDarkMode(isChecked);
        });

        User user = authRepository.getCurrentUser();
        if (user != null) {
            tvUsername.setText("@" + user.username);

            if (user.username != null && !user.username.isEmpty()) {
                tvUserInitial.setText(user.username.substring(0, 1).toUpperCase());
            } else {
                tvUserInitial.setText("U");
            }

            if (!user.username.equals("Guest")) {
                btnLogout.setVisibility(View.VISIBLE);
                btnLogout.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Log Out", (dialog, which) -> {
                            
                            authRepository.logout();
                            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).showLoginScreen();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            } else {
                
                btnLogout.setVisibility(View.GONE);
            }
        }

        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to delete all expenses? This cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    boolean success = expenseService.clearExpenses();
                    if (success) {
                        Toast.makeText(requireContext(), "All expenses deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete expenses", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadDarkModeState() {
        
        int savedMode = prefs.getInt(KEY_DARK_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        boolean isDarkMode = (savedMode == AppCompatDelegate.MODE_NIGHT_YES);

        switchDarkMode.setOnCheckedChangeListener(null);
        switchDarkMode.setChecked(isDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleDarkMode(isChecked);
        });
    }

    private void toggleDarkMode(boolean enable) {
        int newMode = enable ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

        prefs.edit().putInt(KEY_DARK_MODE, newMode).apply();

        AppCompatDelegate.setDefaultNightMode(newMode);

        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private void showEditProfileDialog() {
        User user = authRepository.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etNewUsername = dialogView.findViewById(R.id.etNewUsername);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextView tvError = dialogView.findViewById(R.id.tvError);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        etNewUsername.setText(user.username);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newUsername = etNewUsername.getText() != null ? etNewUsername.getText().toString().trim() : "";
            String currentPassword = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";

            tvError.setVisibility(View.GONE);

            if (newUsername.isEmpty()) {
                tvError.setText("Username cannot be empty");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            boolean usernameChanged = !newUsername.equals(user.username);
            boolean passwordChanged = !newPassword.isEmpty();

            if (passwordChanged && currentPassword.isEmpty()) {
                tvError.setText("Current password is required to change password");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (passwordChanged && newPassword.length() < 3) {
                tvError.setText("New password must be at least 3 characters");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (usernameChanged) {
                if (!authRepository.updateUsername(newUsername)) {
                    tvError.setText("Username already exists or update failed");
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }
                
                tvUsername.setText("@" + newUsername);
                if (!newUsername.isEmpty()) {
                    tvUserInitial.setText(newUsername.substring(0, 1).toUpperCase());
                } else {
                    tvUserInitial.setText("U");
                }
                Toast.makeText(requireContext(), "Username updated successfully", Toast.LENGTH_SHORT).show();
            }

            if (passwordChanged) {
                if (!authRepository.updatePassword(currentPassword, newPassword)) {
                    tvError.setText("Current password is incorrect or update failed");
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }
                Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
            }

            if (!usernameChanged && !passwordChanged) {
                Toast.makeText(requireContext(), "No changes made", Toast.LENGTH_SHORT).show();
            } else if (usernameChanged && passwordChanged) {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private int getMaterialColor(String attrName) {
        if (getContext() == null) return 0xFF000000;
        int attrId = getContext().getResources().getIdentifier(attrName, "attr", getContext().getPackageName());
        if (attrId == 0) {
            attrId = getContext().getResources().getIdentifier(attrName, "attr", "com.google.android.material");
        }
        if (attrId != 0) {
            return getThemeColor(attrId);
        }
        switch (attrName) {
            case "colorSurface":
                return getThemeColor(android.R.attr.colorBackground);
            case "colorPrimary":
                return getThemeColor(android.R.attr.colorPrimary);
            case "colorOnSurfaceVariant":
                return getThemeColor(android.R.attr.textColorSecondary);
            case "colorPrimaryContainer":
                return getThemeColor(android.R.attr.colorPrimary);
            default:
                return 0xFF000000;
        }
    }

    private int getThemeColor(int attr) {
        if (getContext() == null) return 0xFF000000;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            } else if (typedValue.resourceId != 0) {
                return androidx.core.content.ContextCompat.getColor(getContext(), typedValue.resourceId);
            }
        }
        return 0xFF000000;
    }

    @Override
    public void onResume() {
        super.onResume();
        
        loadDarkModeState();

        User user = authRepository.getCurrentUser();
        if (user != null) {
            tvUsername.setText("@" + user.username);
            if (user.username != null && !user.username.isEmpty()) {
                tvUserInitial.setText(user.username.substring(0, 1).toUpperCase());
            } else {
                tvUserInitial.setText("U");
            }
        }
    }
}
