package com.example.myapplication.ui.main;

import com.example.myapplication.R;
import com.example.myapplication.adapters.CategoryBreakdownAdapter;
import com.example.myapplication.services.ExpenseService;
import com.example.myapplication.models.Expense;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyticsFragment extends Fragment {
    
    private RecyclerView rvCategoryBreakdown;     
    private TextView tvTotalExpenses, tvTransactionCount;  
    private TextInputEditText etSearch;           
    private MaterialButton btnSort;               
    private WeeklyOverviewChartView weeklyChart;  
    private ExpenseService expenseService;              

    private CategoryBreakdownAdapter adapter;     
    private List<CategoryBreakdownAdapter.CategoryBreakdown> allBreakdowns;  
    private String currentSortType = "amount_desc";  
    private String searchQuery = "";                

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expenseService = new ExpenseService(requireContext());

        rvCategoryBreakdown = view.findViewById(R.id.rvCategoryBreakdown);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        etSearch = view.findViewById(R.id.etSearchAnalytics);
        btnSort = view.findViewById(R.id.btnSortAnalytics);
        weeklyChart = view.findViewById(R.id.weeklyChart);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                
                searchQuery = s.toString().toLowerCase().trim();
                loadAnalytics();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSort.setOnClickListener(v -> showSortMenu());

        loadAnalytics();
    }

    private int getMaterialColor(String attrName) {
        if (getContext() == null) return 0xFF000000;
        int attrId = getContext().getResources().getIdentifier(attrName, "attr", getContext().getPackageName());
        if (attrId == 0) {
            attrId = getContext().getResources().getIdentifier(attrName, "attr", "com.google.android.material");
        }
        if (attrId != 0) {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(attrId, typedValue, true)) {
                if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT &&
                    typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
                    return typedValue.data;
                } else if (typedValue.resourceId != 0) {
                    return androidx.core.content.ContextCompat.getColor(getContext(), typedValue.resourceId);
                }
            }
        }
        return 0xFF000000;
    }

    private void loadAnalytics() {
        try {
            if (expenseService == null) return;
            
            List<Expense> expenses = expenseService.getExpenses();
            if (expenses == null) expenses = new ArrayList<>();
            
            double total = 0;
            Map<String, Double> categoryTotals = new HashMap<>();
            
            for (Expense expense : expenses) {
                total += expense.amount;
                
                String category = expense.category;
                if (category == null || category.trim().isEmpty()) {
                    category = "Others";
                }
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + expense.amount);
            }

            if (tvTotalExpenses != null) {
                tvTotalExpenses.setText(String.format(Locale.getDefault(), "$%.2f", total));
            }
            if (tvTransactionCount != null) {
                tvTransactionCount.setText(expenses.size() + " transactions");
            }

            if (weeklyChart != null) {
                updateWeeklyChart(expenses);
            }

            allBreakdowns = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                double percentage = total > 0 ? (entry.getValue() / total) * 100 : 0;
                String categoryName = entry.getKey() != null ? entry.getKey() : "Others";
                allBreakdowns.add(new CategoryBreakdownAdapter.CategoryBreakdown(categoryName, entry.getValue(), percentage));
            }

            List<CategoryBreakdownAdapter.CategoryBreakdown> filteredBreakdowns = filterBreakdowns(allBreakdowns);

            List<CategoryBreakdownAdapter.CategoryBreakdown> sortedBreakdowns = sortBreakdowns(filteredBreakdowns);

            if (adapter == null) {
                adapter = new CategoryBreakdownAdapter(sortedBreakdowns);
                LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
                rvCategoryBreakdown.setLayoutManager(layoutManager);
                rvCategoryBreakdown.setAdapter(adapter);
            } else {
                adapter.updateBreakdowns(sortedBreakdowns);
            }
        } catch (Exception e) {
            android.util.Log.e("AnalyticsFragment", "Error loading analytics", e);
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), "Error loading analytics", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateWeeklyChart(List<Expense> expenses) {
        if (weeklyChart == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long todayStart = cal.getTimeInMillis();

        List<String> labels = new ArrayList<>();
        List<Double> totals = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) cal.clone();
            day.setTimeInMillis(todayStart);
            day.add(Calendar.DAY_OF_YEAR, -i);
            String label = new SimpleDateFormat("EEE", Locale.getDefault()).format(day.getTime());
            labels.add(label);
            totals.add(0.0);
        }

        for (Expense expense : expenses) {
            Date d = parseExpenseDate(expense.date);
            if (d == null) continue;

            Calendar expCal = Calendar.getInstance();
            expCal.setTime(d);
            expCal.set(Calendar.HOUR_OF_DAY, 0);
            expCal.set(Calendar.MINUTE, 0);
            expCal.set(Calendar.SECOND, 0);
            expCal.set(Calendar.MILLISECOND, 0);

            long expDay = expCal.getTimeInMillis();
            
            long diffMillis = todayStart - expDay;
            int diffDays = (int) Math.round(diffMillis / (24.0 * 60.0 * 60.0 * 1000.0));
            
            if (diffDays < 0 || diffDays > 6) continue;

            int index = 6 - diffDays;
            if (index >= 0 && index < totals.size()) {
                totals.set(index, totals.get(index) + expense.amount);
            }
        }

        weeklyChart.setData(labels, totals);
    }

    private Date parseExpenseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.equals("Today")) {
            return new Date();
        }

        SimpleDateFormat[] formats = {
            new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()),
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        };

        for (SimpleDateFormat f : formats) {
            try {
                return f.parse(dateStr);
            } catch (ParseException ignored) {}
        }
        return null;
    }

    private List<CategoryBreakdownAdapter.CategoryBreakdown> filterBreakdowns(List<CategoryBreakdownAdapter.CategoryBreakdown> breakdowns) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(breakdowns);
        }

        List<CategoryBreakdownAdapter.CategoryBreakdown> filtered = new ArrayList<>();
        for (CategoryBreakdownAdapter.CategoryBreakdown breakdown : breakdowns) {
            
            if (breakdown.category != null && breakdown.category.toLowerCase().contains(searchQuery)) {
                filtered.add(breakdown);
            } else if (String.format(Locale.getDefault(), "%.2f", breakdown.amount).contains(searchQuery)) {
                filtered.add(breakdown);
            } else if (String.format(Locale.getDefault(), "%.1f", breakdown.percentage).contains(searchQuery)) {
                filtered.add(breakdown);
            }
        }
        return filtered;
    }

    private List<CategoryBreakdownAdapter.CategoryBreakdown> sortBreakdowns(List<CategoryBreakdownAdapter.CategoryBreakdown> breakdowns) {
        List<CategoryBreakdownAdapter.CategoryBreakdown> sorted = new ArrayList<>(breakdowns);
        
        switch (currentSortType) {
            case "amount_desc":
                Collections.sort(sorted, (a, b) -> Double.compare(b.amount, a.amount)); 
                break;
            case "amount_asc":
                Collections.sort(sorted, (a, b) -> Double.compare(a.amount, b.amount)); 
                break;
            case "name_asc":
                Collections.sort(sorted, (a, b) -> {
                    String c1 = a.category != null ? a.category : "";
                    String c2 = b.category != null ? b.category : "";
                    return c1.compareToIgnoreCase(c2);
                });
                break;
            case "name_desc":
                Collections.sort(sorted, (a, b) -> {
                    String c1 = a.category != null ? a.category : "";
                    String c2 = b.category != null ? b.category : "";
                    return c2.compareToIgnoreCase(c1);
                });
                break;
            case "percentage_desc":
                Collections.sort(sorted, (a, b) -> Double.compare(b.percentage, a.percentage)); 
                break;
            case "percentage_asc":
                Collections.sort(sorted, (a, b) -> Double.compare(a.percentage, b.percentage)); 
                break;
        }
        
        return sorted;
    }

    private void showSortMenu() {
        PopupMenu popupMenu = new PopupMenu(requireContext(), btnSort);
        popupMenu.getMenu().add("Amount (High to Low)");
        popupMenu.getMenu().add("Amount (Low to High)");
        popupMenu.getMenu().add("Category (A-Z)");
        popupMenu.getMenu().add("Category (Z-A)");
        popupMenu.getMenu().add("Percentage (High to Low)");
        popupMenu.getMenu().add("Percentage (Low to High)");
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String title = item.getTitle().toString();
                if (title.equals("Amount (High to Low)")) {
                    currentSortType = "amount_desc";
                } else if (title.equals("Amount (Low to High)")) {
                    currentSortType = "amount_asc";
                } else if (title.equals("Category (A-Z)")) {
                    currentSortType = "name_asc";
                } else if (title.equals("Category (Z-A)")) {
                    currentSortType = "name_desc";
                } else if (title.equals("Percentage (High to Low)")) {
                    currentSortType = "percentage_desc";
                } else if (title.equals("Percentage (Low to High)")) {
                    currentSortType = "percentage_asc";
                }
                loadAnalytics();
                return true;
            }
        });
        
        popupMenu.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        loadAnalytics();
    }
}
