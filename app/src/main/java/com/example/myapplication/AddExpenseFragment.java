package com.example.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * AddExpenseFragment - Screen for adding new expenses.
 * 
 * Features:
 * - Category selection with predefined categories and custom "Others" option
 * - Amount input with validation
 * - Optional note field
 * - Date picker for expense date
 * - Budget checking before saving
 * - Navigation back to home after saving
 */
public class AddExpenseFragment extends Fragment {
    // UI Components
    private TextInputEditText etAmount, etNote, etDate, etCustomCategory;
    private com.google.android.material.textfield.TextInputLayout tilCustomCategory;
    private MaterialButton btnSave;
    private GridLayout gridCategories;
    private android.widget.ImageButton btnBack;
    
    // State variables
    private String selectedCategory = "Food";              // Currently selected category
    private String customCategoryName = "";                // Custom category name if "Others" selected
    private TextView othersCategoryLabel;                   // Reference to "Others" category label
    private DataManager dataManager;
    
    // Category data
    private final String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Others"};
    private final String[] categoryIcons = {"🍔", "🚗", "🛍️", "📜", "🍿", "✨"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(requireContext());
        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        etDate = view.findViewById(R.id.etDate);
        etCustomCategory = view.findViewById(R.id.etCustomCategory);
        tilCustomCategory = view.findViewById(R.id.tilCustomCategory);
        btnSave = view.findViewById(R.id.btnSave);
        gridCategories = view.findViewById(R.id.gridCategories);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.nav_home);
            } else if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Set default date
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        // Set up date picker
        etDate.setOnClickListener(v -> showDatePicker());

        // Listen for custom category input changes
        etCustomCategory.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                customCategoryName = s.toString().trim();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Setup category selection grid
        setupCategoryGrid();
        
        // Setup save button click listener
        btnSave.setOnClickListener(v -> saveExpense());
    }

    /**
     * Creates and sets up the category selection grid.
     * Each category is displayed as a card with icon and label.
     * Selecting "Others" shows a text input for custom category name.
     */
    private void setupCategoryGrid() {
        for (int i = 0; i < categories.length; i++) {
            MaterialCardView card = new MaterialCardView(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            params.setMargins(8, 8, 8, 8);
            card.setLayoutParams(params);
            card.setRadius(24);
            card.setCardElevation(2);

            TextView tvIcon = new TextView(requireContext());
            tvIcon.setText(categoryIcons[i]);
            tvIcon.setTextSize(24);
            tvIcon.setPadding(24, 24, 24, 8);
            tvIcon.setGravity(android.view.Gravity.CENTER);

            TextView tvLabel = new TextView(requireContext());
            tvLabel.setText(categories[i]);
            tvLabel.setTextSize(10);
            tvLabel.setGravity(android.view.Gravity.CENTER);
            tvLabel.setPadding(8, 0, 8, 16);
            // Use theme-aware color for text
            int textColor = getMaterialColor("colorOnSurfaceVariant");
            tvLabel.setTextColor(textColor);

            // Store reference to "Others" category label
            if (categories[i].equals("Others")) {
                othersCategoryLabel = tvLabel;
            }

            android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.addView(tvIcon);
            layout.addView(tvLabel);
            card.addView(layout);

            final String category = categories[i];
            card.setOnClickListener(v -> {
                if (category.equals("Others")) {
                    selectedCategory = "Others";
                    // Show the custom category input field
                    tilCustomCategory.setVisibility(View.VISIBLE);
                    etCustomCategory.requestFocus();
                    updateCategorySelection();
                } else {
                    selectedCategory = category;
                    customCategoryName = ""; // Clear custom category when selecting a predefined one
                    // Hide the custom category input field
                    tilCustomCategory.setVisibility(View.GONE);
                    etCustomCategory.setText("");
                    // Reset "Others" label if it was showing custom name
                    if (othersCategoryLabel != null) {
                        othersCategoryLabel.setText("Others");
                    }
                    updateCategorySelection();
                }
            });

            gridCategories.addView(card);
        }
        updateCategorySelection();
    }

    /**
     * Updates the visual appearance of category cards based on selection.
     * Highlights selected category and updates "Others" label if custom name is entered.
     */
    private void updateCategorySelection() {
        // Get theme-aware colors using Material3 attributes
        int surfaceColor = getMaterialColor("colorSurface");
        int primaryColor = getMaterialColor("colorPrimary");
        int onSurfaceVariantColor = getMaterialColor("colorOnSurfaceVariant");
        int primaryContainerColor = getMaterialColor("colorPrimaryContainer");
        
        for (int i = 0; i < gridCategories.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) gridCategories.getChildAt(i);
            android.widget.LinearLayout layout = (android.widget.LinearLayout) card.getChildAt(0);
            TextView tvLabel = (TextView) layout.getChildAt(1);
            
            // Check if this is the "Others" category
            boolean isOthersCategory = (tvLabel == othersCategoryLabel);
            
            // Update "Others" label if custom name is set and it's selected
            if (isOthersCategory) {
                if (selectedCategory.equals("Others") && !customCategoryName.isEmpty()) {
                    tvLabel.setText(customCategoryName);
                } else {
                    tvLabel.setText("Others");
                }
            }
            
            // Determine if this category is selected
            boolean isSelected = false;
            if (isOthersCategory) {
                isSelected = selectedCategory.equals("Others");
            } else {
                // For other categories, compare with the original category name
                String originalCategoryName = categories[i];
                isSelected = originalCategoryName.equals(selectedCategory);
            }
            
            if (isSelected) {
                // Selected category: use primary color background with stroke
                card.setCardBackgroundColor(primaryContainerColor);
                card.setStrokeWidth(4);
                card.setStrokeColor(primaryColor);
                tvLabel.setTextColor(primaryColor);
            } else {
                // Unselected category: use surface color
                card.setCardBackgroundColor(surfaceColor);
                card.setStrokeWidth(0);
                tvLabel.setTextColor(onSurfaceVariantColor);
            }
        }
    }
    
    private int getThemeColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (requireContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && 
                typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            } else {
                return ContextCompat.getColor(requireContext(), typedValue.resourceId);
            }
        }
        // Fallback to a default color if attribute not found
        return 0xFF000000; // Black as fallback
    }
    
    private int getMaterialColor(String attrName) {
        int attrId = requireContext().getResources().getIdentifier(
            attrName, "attr", requireContext().getPackageName());
        if (attrId == 0) {
            // Try Material library package
            attrId = requireContext().getResources().getIdentifier(
                attrName, "attr", "com.google.android.material");
        }
        if (attrId != 0) {
            return getThemeColor(attrId);
        }
        // Fallback colors
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

    /**
     * Shows a date picker dialog.
     * Updates the date field with the selected date formatted as "MMMM d, yyyy".
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // Try to parse existing date if available
        String currentDate = etDate.getText().toString().trim();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDate);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                // If parsing fails, use current date
            }
        }
        
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                etDate.setText(sdf.format(selectedCalendar.getTime()));
            },
            year, month, day
        );
        
        datePickerDialog.show();
    }

    /**
     * Shows an alert when adding an expense would exceed budget.
     * Allows user to proceed anyway or cancel.
     * 
     * @param category Expense category
     * @param budgetCheck Budget check result with limit and spending info
     * @param amount Expense amount
     * @param note Expense note
     * @param date Expense date
     */
    private void showBudgetExceededAlert(String category, DataManager.BudgetCheckResult budgetCheck, double amount, String note, String date) {
        String message = String.format(Locale.getDefault(),
            "Budget Limit Reached!\n\n" +
            "Category: %s\n" +
            "Budget Limit: $%.2f\n" +
            "Current Spent: $%.2f\n" +
            "This Expense: $%.2f\n" +
            "New Total: $%.2f\n\n" +
            "This expense will exceed your budget limit. Do you still want to proceed?",
            category,
            budgetCheck.budgetLimit,
            budgetCheck.currentSpent,
            amount,
            budgetCheck.newTotal
        );

        new AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Budget Limit Exceeded")
            .setMessage(message)
            .setPositiveButton("Save Anyway", (dialog, which) -> {
                // User chose to save despite exceeding budget
                long id = dataManager.addExpense(category, amount, note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date);
                if (id > 0) {
                    Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                    etNote.setText("");
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    etDate.setText(sdf.format(new Date()));
                    selectedCategory = "Food"; // Reset to default
                    customCategoryName = ""; // Clear custom category
                    etCustomCategory.setText(""); // Clear custom category input
                    tilCustomCategory.setVisibility(View.GONE); // Hide custom category input
                    updateCategorySelection();
                    
                    // Navigate to home
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.nav_home);
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    /**
     * Validates input and saves the expense to database.
     * Checks budget before saving and shows alert if budget would be exceeded.
     * Navigates back to home screen on success.
     */
    private void saveExpense() {
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String date = etDate.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate custom category if "Others" is selected
        if (selectedCategory.equals("Others") && customCategoryName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
            etCustomCategory.requestFocus();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use custom category name if "Others" is selected, otherwise use selected category
            String categoryToSave = selectedCategory.equals("Others") ? customCategoryName : selectedCategory;
            
            // Check budget before saving
            DataManager.BudgetCheckResult budgetCheck = dataManager.checkBudget(categoryToSave, amount);
            if (budgetCheck.exceedsBudget) {
                showBudgetExceededAlert(categoryToSave, budgetCheck, amount, note, date);
                return;
            }
            
            long id = dataManager.addExpense(categoryToSave, amount, note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date);
            if (id > 0) {
                Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show();
                etAmount.setText("");
                etNote.setText("");
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                etDate.setText(sdf.format(new Date()));
                selectedCategory = "Food"; // Reset to default
                customCategoryName = ""; // Clear custom category
                etCustomCategory.setText(""); // Clear custom category input
                tilCustomCategory.setVisibility(View.GONE); // Hide custom category input
                updateCategorySelection();
                
                // Navigate to home
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.nav_home);
                }
            } else {
                Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}
