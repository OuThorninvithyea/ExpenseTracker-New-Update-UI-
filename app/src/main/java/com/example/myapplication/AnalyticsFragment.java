package com.example.myapplication;

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

/**
 * AnalyticsFragment - Displays expense analytics and statistics.
 * 
 * Features:
 * - Total expenses and transaction count
 * - Weekly spending chart (last 7 days)
 * - Category breakdown with amounts and percentages
 * - Search functionality to filter categories
 * - Sort options (by amount, name, percentage)
 */
public class AnalyticsFragment extends Fragment {
    // UI Components
    private RecyclerView rvCategoryBreakdown;     // RecyclerView for category breakdown list
    private TextView tvTotalExpenses, tvTransactionCount;  // Display total and count
    private TextInputEditText etSearch;           // Search input
    private MaterialButton btnSort;               // Sort button
    private WeeklyOverviewChartView weeklyChart;  // Custom chart view for weekly spending
    private DataManager dataManager;              // DataManager singleton
    
    // Data and state
    private CategoryBreakdownAdapter adapter;     // Adapter for category breakdown
    private List<CategoryBreakdownAdapter.CategoryBreakdown> allBreakdowns;  // All category breakdowns
    private String currentSortType = "amount_desc";  // Current sort type (default: highest first)
    private String searchQuery = "";                // Current search query

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(requireContext());
        rvCategoryBreakdown = view.findViewById(R.id.rvCategoryBreakdown);
        tvTotalExpenses = view.findViewById(R.id.tvTotalExpenses);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        etSearch = view.findViewById(R.id.etSearchAnalytics);
        btnSort = view.findViewById(R.id.btnSortAnalytics);
        weeklyChart = view.findViewById(R.id.weeklyChart);

        // Setup search
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

        // Setup sort button
        btnSort.setOnClickListener(v -> showSortMenu());

        // Load and display analytics data
        loadAnalytics();
    }

    /**
     * Loads expense data, calculates statistics, and updates UI.
     * Calculates total expenses, transaction count, weekly chart data, and category breakdowns.
     */
    private void loadAnalytics() {
        List<DataManager.Expense> expenses = dataManager.getExpenses();
        
        double total = 0;
        Map<String, Double> categoryTotals = new HashMap<>();
        
        for (DataManager.Expense expense : expenses) {
            total += expense.amount;
            categoryTotals.put(expense.category, 
                categoryTotals.getOrDefault(expense.category, 0.0) + expense.amount);
        }

        tvTotalExpenses.setText(String.format(Locale.getDefault(), "$%.2f", total));
        tvTransactionCount.setText(expenses.size() + " transactions");

        // Weekly chart (last 7 days)
        updateWeeklyChart(expenses);

        // Create category breakdown list
        allBreakdowns = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            double percentage = total > 0 ? (entry.getValue() / total) * 100 : 0;
            allBreakdowns.add(new CategoryBreakdownAdapter.CategoryBreakdown(entry.getKey(), entry.getValue(), percentage));
        }

        // Filter breakdowns based on search query
        List<CategoryBreakdownAdapter.CategoryBreakdown> filteredBreakdowns = filterBreakdowns(allBreakdowns);
        
        // Sort breakdowns
        List<CategoryBreakdownAdapter.CategoryBreakdown> sortedBreakdowns = sortBreakdowns(filteredBreakdowns);

        // Set up RecyclerView with adapter
        if (adapter == null) {
            adapter = new CategoryBreakdownAdapter(sortedBreakdowns);
            LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
            rvCategoryBreakdown.setLayoutManager(layoutManager);
            rvCategoryBreakdown.setAdapter(adapter);
        } else {
            adapter.updateBreakdowns(sortedBreakdowns);
        }
    }

    /**
     * Updates the weekly spending chart with last 7 days of expenses.
     * Groups expenses by day and calculates totals for each day.
     * 
     * @param expenses List of all expenses
     */
    private void updateWeeklyChart(List<DataManager.Expense> expenses) {
        if (weeklyChart == null) return;

        // Build last 7 days buckets (including today), oldest -> newest
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long todayStart = cal.getTimeInMillis();

        List<String> labels = new ArrayList<>();
        List<Double> totals = new ArrayList<>();

        // Prepare 7 days
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) cal.clone();
            day.setTimeInMillis(todayStart);
            day.add(Calendar.DAY_OF_YEAR, -i);
            String label = new SimpleDateFormat("EEE", Locale.getDefault()).format(day.getTime());
            labels.add(label);
            totals.add(0.0);
        }

        // Sum expenses into buckets
        for (DataManager.Expense expense : expenses) {
            Date d = parseExpenseDate(expense.date);
            if (d == null) continue;

            Calendar expCal = Calendar.getInstance();
            expCal.setTime(d);
            expCal.set(Calendar.HOUR_OF_DAY, 0);
            expCal.set(Calendar.MINUTE, 0);
            expCal.set(Calendar.SECOND, 0);
            expCal.set(Calendar.MILLISECOND, 0);

            long expDay = expCal.getTimeInMillis();
            long diffDays = (todayStart - expDay) / (24L * 60L * 60L * 1000L);
            if (diffDays < 0 || diffDays > 6) continue;

            int index = (int) (6 - diffDays);
            totals.set(index, totals.get(index) + expense.amount);
        }

        weeklyChart.setData(labels, totals);
    }

    /**
     * Parses expense date string into Date object.
     * Supports multiple date formats and "Today" keyword.
     * 
     * @param dateStr Date string to parse
     * @return Parsed Date object, or current date if parsing fails
     */
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

    /**
     * Filters category breakdowns based on search query.
     * Searches in category name, amount, and percentage.
     * 
     * @param breakdowns List of breakdowns to filter
     * @return Filtered list matching search query
     */
    private List<CategoryBreakdownAdapter.CategoryBreakdown> filterBreakdowns(List<CategoryBreakdownAdapter.CategoryBreakdown> breakdowns) {
        if (searchQuery.isEmpty()) {
            return new ArrayList<>(breakdowns);
        }

        List<CategoryBreakdownAdapter.CategoryBreakdown> filtered = new ArrayList<>();
        for (CategoryBreakdownAdapter.CategoryBreakdown breakdown : breakdowns) {
            // Search in category name, amount, and percentage
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

    /**
     * Sorts category breakdowns based on current sort type.
     * Supports sorting by amount, category name, or percentage (ascending/descending).
     * 
     * @param breakdowns List of breakdowns to sort
     * @return Sorted list
     */
    private List<CategoryBreakdownAdapter.CategoryBreakdown> sortBreakdowns(List<CategoryBreakdownAdapter.CategoryBreakdown> breakdowns) {
        List<CategoryBreakdownAdapter.CategoryBreakdown> sorted = new ArrayList<>(breakdowns);
        
        switch (currentSortType) {
            case "amount_desc":
                Collections.sort(sorted, (a, b) -> Double.compare(b.amount, a.amount)); // Highest first
                break;
            case "amount_asc":
                Collections.sort(sorted, (a, b) -> Double.compare(a.amount, b.amount)); // Lowest first
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
                Collections.sort(sorted, (a, b) -> Double.compare(b.percentage, a.percentage)); // Highest first
                break;
            case "percentage_asc":
                Collections.sort(sorted, (a, b) -> Double.compare(a.percentage, b.percentage)); // Lowest first
                break;
        }
        
        return sorted;
    }

    /**
     * Shows popup menu with sort options.
     * Updates sort type and reloads data when option selected.
     */
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
