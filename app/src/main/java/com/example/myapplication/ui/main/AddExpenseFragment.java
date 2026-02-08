package com.example.myapplication.ui.main;

import com.example.myapplication.R;
import com.example.myapplication.R;
import com.example.myapplication.services.ExpenseService;
import com.example.myapplication.models.BudgetCheckResult;
import com.example.myapplication.utils.CurrencyHelper;

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

public class AddExpenseFragment extends Fragment {
    
    private TextInputEditText etAmount, etNote, etDate, etCustomCategory;
    private com.google.android.material.textfield.TextInputLayout tilCustomCategory;
    private MaterialButton btnSave;
    private GridLayout gridCategories;
    private android.widget.ImageButton btnBack;

    private String selectedCategory = "Food";              
    private String customCategoryName = "";                
    private TextView othersCategoryLabel;                   
    private ExpenseService expenseService;

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

        expenseService = new ExpenseService(requireContext());

        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        etDate = view.findViewById(R.id.etDate);
        etCustomCategory = view.findViewById(R.id.etCustomCategory);
        tilCustomCategory = view.findViewById(R.id.tilCustomCategory);
        btnSave = view.findViewById(R.id.btnSave);
        gridCategories = view.findViewById(R.id.gridCategories);
        btnBack = view.findViewById(R.id.btnBack);

        TextView tvCurrencySymbol = view.findViewById(R.id.tvCurrencySymbol);
        if (tvCurrencySymbol != null) {
            tvCurrencySymbol.setText(CurrencyHelper.getCurrencySymbol(requireContext()));
        }

        btnBack.setOnClickListener(v -> {
            
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                
                getParentFragmentManager().popBackStack();
            } else if (getActivity() instanceof MainActivity) {
                
                ((MainActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.nav_home);
            } else if (getActivity() != null) {
                
                getActivity().onBackPressed();
            }
        });

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        etDate.setText(sdf.format(new Date()));

        etDate.setOnClickListener(v -> showDatePicker());

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

        setupCategoryGrid();

        btnSave.setOnClickListener(v -> saveExpense());
    }

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
            
            int textColor = getMaterialColor("colorOnSurfaceVariant");
            tvLabel.setTextColor(textColor);

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
                    
                    tilCustomCategory.setVisibility(View.VISIBLE);
                    etCustomCategory.requestFocus();
                    updateCategorySelection();
                } else {
                    selectedCategory = category;
                    customCategoryName = ""; 
                    
                    tilCustomCategory.setVisibility(View.GONE);
                    etCustomCategory.setText("");
                    
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

    private void updateCategorySelection() {
        try {
            if (gridCategories == null) return;

            int surfaceColor = getMaterialColor("colorSurface");
            int primaryColor = getMaterialColor("colorPrimary");
            int onSurfaceVariantColor = getMaterialColor("colorOnSurfaceVariant");
            int primaryContainerColor = getMaterialColor("colorPrimaryContainer");
            
            for (int i = 0; i < gridCategories.getChildCount(); i++) {
                View child = gridCategories.getChildAt(i);
                if (!(child instanceof MaterialCardView)) continue;
                
                MaterialCardView card = (MaterialCardView) child;
                if (card.getChildCount() == 0) continue;
                
                View grandChild = card.getChildAt(0);
                if (!(grandChild instanceof android.widget.LinearLayout)) continue;
                
                android.widget.LinearLayout layout = (android.widget.LinearLayout) grandChild;
                if (layout.getChildCount() < 2) continue;
                
                View labelView = layout.getChildAt(1);
                if (!(labelView instanceof TextView)) continue;
                
                TextView tvLabel = (TextView) labelView;

                boolean isOthersCategory = (tvLabel == othersCategoryLabel);

                if (isOthersCategory) {
                    if ("Others".equals(selectedCategory) && !customCategoryName.isEmpty()) {
                        tvLabel.setText(customCategoryName);
                    } else {
                        tvLabel.setText("Others");
                    }
                }

                boolean isSelected = false;
                if (isOthersCategory) {
                    isSelected = "Others".equals(selectedCategory);
                } else {
                    
                    if (categories != null && i < categories.length) {
                        String originalCategoryName = categories[i];
                        isSelected = originalCategoryName.equals(selectedCategory);
                    }
                }
                
                if (isSelected) {
                    
                    card.setCardBackgroundColor(primaryContainerColor);
                    card.setStrokeWidth(4);
                    card.setStrokeColor(primaryColor);
                    tvLabel.setTextColor(primaryColor);
                } else {
                    
                    card.setCardBackgroundColor(surfaceColor);
                    card.setStrokeWidth(0);
                    tvLabel.setTextColor(onSurfaceVariantColor);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AddExpenseFragment", "Error updating category selection", e);
        }
    }

    private int getThemeColor(int attr) {
        if (getContext() == null) return 0xFF000000;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        try {
            
            if (getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
                
                if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && 
                    typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                    return typedValue.data;
                } else if (typedValue.resourceId != 0) {
                    
                    return ContextCompat.getColor(getContext(), typedValue.resourceId);
                }
            }
        } catch (Exception e) {
            
        }
        
        return 0xFF000000; 
    }

    private int getMaterialColor(String attrName) {
        if (getContext() == null) return 0xFF000000;
        
        int attrId = 0;
        try {
            
            attrId = getContext().getResources().getIdentifier(
                attrName, "attr", getContext().getPackageName());
            
            if (attrId == 0) {
                
                attrId = getContext().getResources().getIdentifier(
                    attrName, "attr", "com.google.android.material");
            }
            
            if (attrId == 0) {
                 
                 attrId = getContext().getResources().getIdentifier(attrName, "attr", "android");
            }
        } catch (Exception e) {
             
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

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        String currentDate = etDate.getText().toString().trim();
        if (!currentDate.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDate);
                if (date != null) {
                    calendar.setTime(date);
                }
            } catch (Exception e) {
                
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

    private void showBudgetExceededAlert(String category, BudgetCheckResult budgetCheck, double amount, String note, String date) {
        String message = String.format(Locale.getDefault(),
            "Budget Limit Reached!\n\n" +
            "Category: %s\n" +
            "Budget Limit: %s\n" +
            "Current Spent: %s\n" +
            "This Expense: %s\n" +
            "New Total: %s\n\n" +
            "This expense will exceed your budget limit. Do you still want to proceed?",
            category,
            CurrencyHelper.formatCurrency(requireContext(), budgetCheck.budgetLimit),
            CurrencyHelper.formatCurrency(requireContext(), budgetCheck.currentSpent),
            CurrencyHelper.formatCurrency(requireContext(), amount),
            CurrencyHelper.formatCurrency(requireContext(), budgetCheck.newTotal)
        );

        new AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Budget Limit Exceeded")
            .setMessage(message)
            .setPositiveButton("Save Anyway", (dialog, which) -> {
                
                long id = expenseService.addExpense(category, amount, note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date);
                if (id > 0) {
                    Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                    etNote.setText("");
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    etDate.setText(sdf.format(new Date()));
                    selectedCategory = "Food"; 
                    customCategoryName = ""; 
                    etCustomCategory.setText(""); 
                    tilCustomCategory.setVisibility(View.GONE); 
                    updateCategorySelection();

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

    private void saveExpense() {
        try {
            if (etAmount == null || etNote == null || etDate == null) return;

            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("Others".equals(selectedCategory) && customCategoryName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
                if (etCustomCategory != null) etCustomCategory.requestFocus();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            String categoryToSave = "Others".equals(selectedCategory) ? customCategoryName : selectedCategory;

            if (expenseService != null) {
                BudgetCheckResult budgetCheck = expenseService.checkBudget(categoryToSave, amount);
                if (budgetCheck.exceedsBudget) {
                    showBudgetExceededAlert(categoryToSave, budgetCheck, amount, note, date);
                    return;
                }
                
                long id = expenseService.addExpense(categoryToSave, amount, note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date);
                if (id > 0) {
                    Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                    etNote.setText("");
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                    etDate.setText(sdf.format(new Date()));
                    selectedCategory = "Food"; 
                    customCategoryName = ""; 
                    if (etCustomCategory != null) etCustomCategory.setText(""); 
                    if (tilCustomCategory != null) tilCustomCategory.setVisibility(View.GONE); 
                    updateCategorySelection();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).bottomNavigation.setSelectedItemId(R.id.nav_home);
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("AddExpenseFragment", "Error saving expense", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error saving expense", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
