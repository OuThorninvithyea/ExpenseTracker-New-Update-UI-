package com.example.myapplication.ui.main;

import com.example.myapplication.R;
import com.example.myapplication.adapters.ExpenseAdapter;
import com.example.myapplication.services.ExpenseService;
import com.example.myapplication.models.BudgetCheckResult;
import com.example.myapplication.models.Expense;
import com.example.myapplication.utils.CurrencyHelper;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    
    private RecyclerView rvExpenses;              
    private TextView tvTotalAmount;               
    private ExpenseAdapter adapter;                
    private ExpenseService expenseService;              
    private TextInputEditText etSearch;           
    private MaterialButton btnSort;                
    private FloatingActionButton fabAddExpense;  

    private List<Expense> allExpenses; 
    private String currentSortType = "date_desc";  
    private String searchQuery = "";               

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            
            expenseService = new ExpenseService(requireContext());

            rvExpenses = view.findViewById(R.id.rvExpenses);
            tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
            etSearch = view.findViewById(R.id.etSearch);
            btnSort = view.findViewById(R.id.btnSort);
            fabAddExpense = view.findViewById(R.id.fabAddExpense);

            if (rvExpenses == null || tvTotalAmount == null || etSearch == null || btnSort == null || fabAddExpense == null) {
                android.util.Log.e("HomeFragment", "One or more views not found in layout");
                Toast.makeText(requireContext(), "Error: View initialization failed", Toast.LENGTH_LONG).show();
                return;
            }

            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        searchQuery = s.toString().toLowerCase().trim();
                        loadExpenses();
                    } catch (Exception e) {
                        android.util.Log.e("HomeFragment", "Error in search text change", e);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnSort.setOnClickListener(v -> {
                try {
                    showSortMenu();
                } catch (Exception e) {
                    android.util.Log.e("HomeFragment", "Error showing sort menu", e);
                    Toast.makeText(requireContext(), "Error showing options", Toast.LENGTH_SHORT).show();
                }
            });

            fabAddExpense.setOnClickListener(v -> {
                try {
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new AddExpenseFragment())
                            .addToBackStack(null)
                            .commit();
                    }
                } catch (Exception e) {
                    android.util.Log.e("HomeFragment", "Error navigating to add expense", e);
                    Toast.makeText(requireContext(), "Error opening add screen", Toast.LENGTH_SHORT).show();
                }
            });

            adapter = new ExpenseAdapter(new ArrayList<>(), new ExpenseAdapter.OnExpenseClickListener() {
                @Override
                public void onEditClick(Expense expense) {
                    try {
                        showEditDialog(expense);
                    } catch (Exception e) {
                        android.util.Log.e("HomeFragment", "Error showing edit dialog", e);
                        Toast.makeText(requireContext(), "Error opening edit dialog", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDeleteClick(Expense expense) {
                    try {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Expense")
                            .setMessage("Are you sure to delete it?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                try {
                                    if (expenseService.deleteExpense(expense.id)) {
                                        loadExpenses();
                                        Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("HomeFragment", "Error deleting expense", e);
                                    Toast.makeText(requireContext(), "Error deleting expense", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    } catch (Exception e) {
                        android.util.Log.e("HomeFragment", "Error showing delete dialog", e);
                    }
                }
            });

            rvExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvExpenses.setAdapter(adapter);

            loadExpenses();
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error in onViewCreated", e);
            
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error initializing home screen: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadExpenses() {
        try {
            if (expenseService == null) return;
            
            allExpenses = expenseService.getExpenses();

            List<Expense> filteredExpenses = filterExpenses(allExpenses);

            List<Expense> sortedExpenses = sortExpenses(filteredExpenses);
            
            if (adapter != null) {
                adapter.updateExpenses(sortedExpenses);
            }

            double total = 0;
            for (Expense expense : sortedExpenses) {
                total += expense.amount;
            }
            
            if (tvTotalAmount != null) {
                tvTotalAmount.setText(CurrencyHelper.formatCurrency(requireContext(), total));
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "Error loading expenses", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<Expense> filterExpenses(List<Expense> expenses) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(expenses);
        }

        List<Expense> filtered = new ArrayList<>();
        for (Expense expense : expenses) {
            
            if (expense.note != null && expense.note.toLowerCase().contains(searchQuery)) {
                filtered.add(expense);
            } else if (expense.category != null && expense.category.toLowerCase().contains(searchQuery)) {
                filtered.add(expense);
            } else if (String.format(Locale.getDefault(), "%.2f", expense.amount).contains(searchQuery)) {
                filtered.add(expense);
            } else if (expense.date != null && expense.date.toLowerCase().contains(searchQuery)) {
                filtered.add(expense);
            }
        }
        return filtered;
    }

    private List<Expense> sortExpenses(List<Expense> expenses) {
        List<Expense> sorted = new ArrayList<>(expenses);
        
        switch (currentSortType) {
            case "date_desc":
                Collections.sort(sorted, (e1, e2) -> {
                    Date d1 = parseDate(e1.date);
                    Date d2 = parseDate(e2.date);
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); 
                });
                break;
            case "date_asc":
                Collections.sort(sorted, (e1, e2) -> {
                    Date d1 = parseDate(e1.date);
                    Date d2 = parseDate(e2.date);
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d1.compareTo(d2); 
                });
                break;
            case "amount_desc":
                Collections.sort(sorted, (e1, e2) -> Double.compare(e2.amount, e1.amount)); 
                break;
            case "amount_asc":
                Collections.sort(sorted, (e1, e2) -> Double.compare(e1.amount, e2.amount)); 
                break;
            case "category_asc":
                Collections.sort(sorted, (e1, e2) -> {
                    String c1 = e1.category != null ? e1.category : "";
                    String c2 = e2.category != null ? e2.category : "";
                    return c1.compareToIgnoreCase(c2);
                });
                break;
            case "category_desc":
                Collections.sort(sorted, (e1, e2) -> {
                    String c1 = e1.category != null ? e1.category : "";
                    String c2 = e2.category != null ? e2.category : "";
                    return c2.compareToIgnoreCase(c1);
                });
                break;
        }
        
        return sorted;
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("Today")) {
            return new Date(); 
        }
        
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()),
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        };
        
        for (SimpleDateFormat format : formats) {
            try {
                return format.parse(dateStr);
            } catch (ParseException e) {
                
            }
        }
        return null;
    }

    private void showSortMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), btnSort);
        popupMenu.getMenu().add("Date (Newest First)");
        popupMenu.getMenu().add("Date (Oldest First)");
        popupMenu.getMenu().add("Amount (High to Low)");
        popupMenu.getMenu().add("Amount (Low to High)");
        popupMenu.getMenu().add("Category (A-Z)");
        popupMenu.getMenu().add("Category (Z-A)");
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String title = item.getTitle().toString();
                if (title.equals("Date (Newest First)")) {
                    currentSortType = "date_desc";
                } else if (title.equals("Date (Oldest First)")) {
                    currentSortType = "date_asc";
                } else if (title.equals("Amount (High to Low)")) {
                    currentSortType = "amount_desc";
                } else if (title.equals("Amount (Low to High)")) {
                    currentSortType = "amount_asc";
                } else if (title.equals("Category (A-Z)")) {
                    currentSortType = "category_asc";
                } else if (title.equals("Category (Z-A)")) {
                    currentSortType = "category_desc";
                }
                loadExpenses();
                return true;
            }
        });
        
        popupMenu.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadExpenses();
    }

    private void showEditDialog(Expense expense) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_expense, null);
        
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etNote = dialogView.findViewById(R.id.etNote);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);
        GridLayout gridCategories = dialogView.findViewById(R.id.gridCategories);

        etAmount.setText(String.valueOf(expense.amount));
        etNote.setText(expense.note);
        etDate.setText(expense.date);

        etDate.setOnClickListener(v -> showDatePickerDialog(etDate, expense.date));
        
        String[] categories = {"Food", "Transport", "Shopping", "Bills", "Entertainment", "Others"};
        String[] categoryIcons = {"🍔", "🚗", "🛍️", "📜", "🍿", "✨"};
        String[] selectedCategory = {expense.category};

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
            tvLabel.setTextColor(requireContext().getColor(android.R.color.darker_gray));

            android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.addView(tvIcon);
            layout.addView(tvLabel);
            card.addView(layout);

            final String category = categories[i];
            card.setOnClickListener(v -> {
                selectedCategory[0] = category;
                updateCategorySelection(gridCategories, categories, selectedCategory[0]);
            });

            gridCategories.addView(card);
        }
        updateCategorySelection(gridCategories, categories, selectedCategory[0]);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(dialogView)
            .setPositiveButton("Save", (d, w) -> {
                String amountStr = etAmount.getText().toString().trim();
                String note = etNote.getText().toString().trim();
                String date = etDate.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!selectedCategory[0].equals(expense.category) || amount != expense.amount) {
                        BudgetCheckResult budgetCheck;
                        if (!selectedCategory[0].equals(expense.category)) {
                            
                            budgetCheck = expenseService.checkBudget(selectedCategory[0], amount);
                        } else {
                            
                            budgetCheck = expenseService.checkBudgetOnUpdate(selectedCategory[0], amount, expense.id);
                        }
                        
                        if (budgetCheck.exceedsBudget) {
                            showBudgetExceededAlert(selectedCategory[0], budgetCheck, amount, expense, note, date);
                            return;
                        }
                    }

                    if (expenseService.updateExpense(expense.id, selectedCategory[0], amount, 
                            note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date)) {
                        loadExpenses();
                        Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to update expense", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .create();

        dialog.show();
    }

    private void showDatePickerDialog(TextInputEditText etDate, String currentDateStr) {
        Calendar calendar = Calendar.getInstance();

        if (currentDateStr != null && !currentDateStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
                Date date = sdf.parse(currentDateStr);
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

    private void showBudgetExceededAlert(String category, BudgetCheckResult budgetCheck, double amount, Expense expense, String note, String date) {
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
            .setPositiveButton("Update Anyway", (dialog, which) -> {
                
                if (expenseService.updateExpense(expense.id, category, amount, 
                        note.isEmpty() ? "No note" : note, date.isEmpty() ? "Today" : date)) {
                    loadExpenses();
                    Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update expense", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void updateCategorySelection(GridLayout gridCategories, String[] categories, String selected) {
        int surfaceColor = getMaterialColor("colorSurface");
        int primaryColor = getMaterialColor("colorPrimary");
        int primaryContainerColor = getMaterialColor("colorPrimaryContainer");
        int onSurfaceVariantColor = getMaterialColor("colorOnSurfaceVariant");

        for (int i = 0; i < gridCategories.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) gridCategories.getChildAt(i);
            TextView tvLabel = (TextView) ((android.widget.LinearLayout) card.getChildAt(0)).getChildAt(1);
            if (tvLabel.getText().toString().equals(selected)) {
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
    }

    private int getThemeColor(int attr) {
        if (getContext() == null) return 0xFF000000;
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            } else if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(getContext(), typedValue.resourceId);
            }
        }
        return 0xFF000000;
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
}
