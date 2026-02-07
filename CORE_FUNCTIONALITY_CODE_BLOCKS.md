# Core Functionality Code Blocks - Expense Tracker App

## 🎯 Focus: Expense Management, Budget Tracking, Analytics (NO LOGIN/SIGNUP)

---

## 📋 Table of Contents

1. [Adding an Expense](#1-adding-an-expense)
2. [Loading & Displaying Expenses](#2-loading--displaying-expenses)
3. [Search & Filter Expenses](#3-search--filter-expenses)
4. [Sort Expenses](#4-sort-expenses)
5. [Edit Expense](#5-edit-expense)
6. [Delete Expense](#6-delete-expense)
7. [Budget Checking Logic](#7-budget-checking-logic)
8. [Set/Update Budget](#8-setupdate-budget)
9. [Load Budgets with Spending](#9-load-budgets-with-spending)
10. [Analytics Calculation](#10-analytics-calculation)
11. [Weekly Chart Data](#11-weekly-chart-data)
12. [RecyclerView Adapter Pattern](#12-recyclerview-adapter-pattern)

---

## 1. Adding an Expense

**Location:** `AddExpenseFragment.java` - `saveExpense()` method  
**Lines:** 421-480

**What it does:**
- Validates user input (amount, category)
- Checks if expense would exceed budget
- Saves expense to database
- Navigates back to home screen

**Key Code Block:**
```java
private void saveExpense() {
    // Get input values
    String amountStr = etAmount.getText().toString().trim();
    String note = etNote.getText().toString().trim();
    String date = etDate.getText().toString().trim();

    // Validate amount
    if (amountStr.isEmpty()) {
        Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
        return;
    }

    // Parse amount
    double amount = Double.parseDouble(amountStr);
    
    // Check budget before saving
    DataManager.BudgetCheckResult budgetCheck = dataManager.checkBudget(categoryToSave, amount);
    if (budgetCheck.exceedsBudget) {
        showBudgetExceededAlert(...); // Warn user
        return;
    }
    
    // Save to database
    long id = dataManager.addExpense(categoryToSave, amount, note, date);
}
```

**What to Say:**
> "When saving an expense, I first validate the amount input. Then I check if adding this expense would exceed the budget for that category. If it would exceed, I show a warning dialog. If not, I save it to the database using DataManager, which stores it in SQLite."

---

## 2. Loading & Displaying Expenses

**Location:** `HomeFragment.java` - `loadExpenses()` method  
**Lines:** 151-169

**What it does:**
- Gets all expenses from database
- Applies search filter
- Applies sorting
- Updates RecyclerView
- Calculates and displays total

**Key Code Block:**
```java
private void loadExpenses() {
    // Get all expenses from database
    allExpenses = dataManager.getExpenses();
    
    // Filter expenses based on search query
    List<DataManager.Expense> filteredExpenses = filterExpenses(allExpenses);
    
    // Sort expenses
    List<DataManager.Expense> sortedExpenses = sortExpenses(filteredExpenses);
    
    // Update RecyclerView adapter
    adapter.updateExpenses(sortedExpenses);

    // Calculate total from filtered expenses
    double total = 0;
    for (DataManager.Expense expense : sortedExpenses) {
        total += expense.amount;
    }
    // Display total
    tvTotalAmount.setText(String.format(Locale.getDefault(), "$%.2f", total));
}
```

**What to Say:**
> "The `loadExpenses()` method retrieves all expenses from the database, then applies filtering based on the search query, sorts them according to user preference, and updates the RecyclerView. It also calculates the total of all displayed expenses and shows it at the top."

---

## 3. Search & Filter Expenses

**Location:** `HomeFragment.java` - `filterExpenses()` method  
**Lines:** 178-197

**What it does:**
- Searches through expense fields (note, category, amount, date)
- Returns matching expenses
- Case-insensitive search

**Key Code Block:**
```java
private List<DataManager.Expense> filterExpenses(List<DataManager.Expense> expenses) {
    if (searchQuery.isEmpty()) {
        return new ArrayList<>(expenses); // Return all if no search
    }

    List<DataManager.Expense> filtered = new ArrayList<>();
    for (DataManager.Expense expense : expenses) {
        // Search in note, category, amount, and date
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
```

**What to Say:**
> "The filter method loops through all expenses and checks if the search query matches any field - note, category, amount, or date. It uses case-insensitive matching by converting everything to lowercase. If a match is found, the expense is added to the filtered list."

---

## 4. Sort Expenses

**Location:** `HomeFragment.java` - `sortExpenses()` method  
**Lines:** 206-253

**What it does:**
- Sorts expenses by date, amount, or category
- Supports ascending and descending order
- Uses Comparator for sorting

**Key Code Block:**
```java
private List<DataManager.Expense> sortExpenses(List<DataManager.Expense> expenses) {
    List<DataManager.Expense> sorted = new ArrayList<>(expenses);
    
    switch (currentSortType) {
        case "date_desc":
            // Sort by date, newest first
            Collections.sort(sorted, (e1, e2) -> {
                Date d1 = parseDate(e1.date);
                Date d2 = parseDate(e2.date);
                return d2.compareTo(d1); // Newest first
            });
            break;
        case "amount_desc":
            // Sort by amount, highest first
            Collections.sort(sorted, (e1, e2) -> 
                Double.compare(e2.amount, e1.amount));
            break;
        // ... other sort types
    }
    return sorted;
}
```

**What to Say:**
> "The sort method uses a switch statement to determine the sort type. It uses Java's Collections.sort() with a Comparator lambda function. For dates, I parse the date strings first, then compare them. For amounts, I use Double.compare(). The result is a sorted list based on the user's preference."

---

## 5. Edit Expense

**Location:** `HomeFragment.java` - `showEditDialog()` method  
**Lines:** 339-452

**What it does:**
- Shows dialog with pre-filled expense data
- Allows user to modify category, amount, note, date
- Checks budget before updating
- Updates database on save

**Key Code Block:**
```java
private void showEditDialog(DataManager.Expense expense) {
    // Pre-fill fields with existing values
    etAmount.setText(String.valueOf(expense.amount));
    etNote.setText(expense.note);
    etDate.setText(expense.date);
    
    // On save button click
    .setPositiveButton("Save", (d, w) -> {
        // Get new values
        double amount = Double.parseDouble(amountStr);
        
        // Check budget (exclude current expense from calculation)
        DataManager.BudgetCheckResult budgetCheck = 
            dataManager.checkBudgetOnUpdate(category, amount, expense.id);
        
        if (budgetCheck.exceedsBudget) {
            showBudgetExceededAlert(...);
            return;
        }
        
        // Update in database
        dataManager.updateExpense(expense.id, category, amount, note, date);
        loadExpenses(); // Refresh list
    })
}
```

**What to Say:**
> "When editing, I pre-fill the dialog with the expense's current values. When saving, I check the budget using `checkBudgetOnUpdate()` which excludes the current expense from the calculation. If the update would exceed the budget, I warn the user. Otherwise, I update the database and refresh the expense list."

---

## 6. Delete Expense

**Location:** `HomeFragment.java` - `onDeleteClick()` callback  
**Lines:** 125-137

**What it does:**
- Shows confirmation dialog
- Deletes expense from database
- Refreshes the list

**Key Code Block:**
```java
@Override
public void onDeleteClick(DataManager.Expense expense) {
    new AlertDialog.Builder(requireContext())
        .setTitle("Delete Expense")
        .setMessage("Are you sure to delete it?")
        .setPositiveButton("Delete", (dialog, which) -> {
            if (dataManager.deleteExpense(expense.id)) {
                loadExpenses(); // Refresh list
                Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
            }
        })
        .setNegativeButton("Cancel", null)
        .show();
}
```

**What to Say:**
> "When deleting, I first show a confirmation dialog to prevent accidental deletions. If the user confirms, I call `dataManager.deleteExpense()` with the expense ID, which removes it from the database. Then I refresh the expense list to show the updated data."

---

## 7. Budget Checking Logic

**Location:** `DataManager.java` - `checkBudget()` method  
**Lines:** 427-458

**What it does:**
- Finds budget for a category
- Calculates total already spent in that category
- Adds new expense amount
- Compares with budget limit
- Returns result with warning status

**Key Code Block:**
```java
public BudgetCheckResult checkBudget(String category, double amount) {
    // Find budget for this category
    List<Budget> budgets = getBudgets();
    Budget budget = null;
    for (Budget b : budgets) {
        if (b.category.equals(category)) {
            budget = b;
            break;
        }
    }
    
    // If no budget set, no check needed
    if (budget == null) {
        return new BudgetCheckResult(false, 0, 0, 0);
    }
    
    // Calculate current total spent for this category
    List<Expense> expenses = getExpenses();
    double totalSpent = 0;
    for (Expense expense : expenses) {
        if (expense.category.equals(category)) {
            totalSpent += expense.amount;
        }
    }
    
    // Calculate new total if this expense is added
    double newTotal = totalSpent + amount;
    boolean exceedsBudget = newTotal >= budget.limit;
    
    return new BudgetCheckResult(exceedsBudget, budget.limit, totalSpent, newTotal);
}
```

**What to Say:**
> "Budget checking works in three steps: First, I find the budget for the category. Then I loop through all expenses in that category to calculate the total already spent. Finally, I add the new expense amount and compare it with the budget limit. If it exceeds, I return a warning result with all the details."

---

## 8. Set/Update Budget

**Location:** `DataManager.java` - `setBudget()` method  
**Lines:** 370-374

**What it does:**
- Sets or updates budget limit for a category
- Uses INSERT OR REPLACE in database
- One budget per category per user

**Key Code Block:**
```java
public boolean setBudget(String category, double limit) {
    int userId = prefs.getInt("userId", -1);
    if (userId <= 0) return false;
    return dbHelper.setBudget(userId, category, limit);
}
```

**Database Implementation:**
```java
// In DatabaseHelper.java
public boolean setBudget(int userId, String category, double limit) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COL_BUDGET_USER_ID, userId);
    values.put(COL_BUDGET_CATEGORY, category);
    values.put(COL_BUDGET_LIMIT, limit);

    // INSERT OR REPLACE - updates if exists, inserts if new
    long id = db.insertWithOnConflict(TABLE_BUDGETS, null, values, 
        SQLiteDatabase.CONFLICT_REPLACE);
    return id > 0;
}
```

**What to Say:**
> "Setting a budget uses `insertWithOnConflict()` with `CONFLICT_REPLACE`, which means if a budget already exists for that category, it updates it. If not, it creates a new one. This ensures each category has only one budget limit."

---

## 9. Load Budgets with Spending

**Location:** `BudgetFragment.java` - `loadBudgets()` method  
**Lines:** 111-117

**What it does:**
- Gets all budgets from database
- Gets all expenses
- Calculates spent amount per category
- Creates BudgetItem objects with both budget and spent amount
- Updates RecyclerView

**Key Code Block:**
```java
private void loadBudgets() {
    // Get budgets and expenses
    List<DataManager.Budget> budgets = dataManager.getBudgets();
    List<DataManager.Expense> expenses = dataManager.getExpenses();
    
    // Calculate spent amounts per category
    Map<String, Double> categoryTotals = new HashMap<>();
    for (DataManager.Expense expense : expenses) {
        double currentTotal = categoryTotals.getOrDefault(expense.category, 0.0);
        categoryTotals.put(expense.category, currentTotal + expense.amount);
    }
    
    // Create budget items with spent amounts
    budgetItems.clear();
    for (DataManager.Budget budget : budgets) {
        double spent = categoryTotals.getOrDefault(budget.category, 0.0);
        budgetItems.add(new BudgetAdapter.BudgetItem(budget, spent));
    }
    
    adapter.updateBudgets(budgetItems);
}
```

**What to Say:**
> "To display budgets with spending, I first get all budgets and expenses. Then I use a HashMap to calculate the total spent per category by looping through expenses. Finally, I create BudgetItem objects that combine the budget limit with the actual spent amount, which allows the adapter to show progress bars and warnings."

---

## 10. Analytics Calculation

**Location:** `AnalyticsFragment.java` - `loadAnalytics()` method  
**Lines:** 119-159

**What it does:**
- Calculates total expenses
- Counts transactions
- Groups expenses by category
- Calculates percentage per category
- Updates weekly chart
- Displays category breakdown

**Key Code Block:**
```java
private void loadAnalytics() {
    List<DataManager.Expense> expenses = dataManager.getExpenses();
    
    double total = 0;
    Map<String, Double> categoryTotals = new HashMap<>();
    
    // Calculate totals
    for (DataManager.Expense expense : expenses) {
        total += expense.amount;
        categoryTotals.put(expense.category, 
            categoryTotals.getOrDefault(expense.category, 0.0) + expense.amount);
    }

    tvTotalExpenses.setText(String.format(Locale.getDefault(), "$%.2f", total));
    tvTransactionCount.setText(expenses.size() + " transactions");

    // Update weekly chart
    updateWeeklyChart(expenses);

    // Create category breakdown with percentages
    allBreakdowns = new ArrayList<>();
    for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
        double percentage = total > 0 ? (entry.getValue() / total) * 100 : 0;
        allBreakdowns.add(new CategoryBreakdownAdapter.CategoryBreakdown(
            entry.getKey(), entry.getValue(), percentage));
    }
    
    adapter.updateBreakdowns(sortedBreakdowns);
}
```

**What to Say:**
> "Analytics calculation loops through all expenses to calculate the total and group them by category using a HashMap. For each category, I calculate its percentage of the total expenses. I also update the weekly chart with the last 7 days of data. The results are displayed in the RecyclerView showing category, amount, and percentage."

---

## 11. Weekly Chart Data

**Location:** `AnalyticsFragment.java` - `updateWeeklyChart()` method  
**Lines:** 167-193

**What it does:**
- Creates 7-day buckets (last 7 days including today)
- Groups expenses by day
- Calculates total spent per day
- Updates custom chart view

**Key Code Block:**
```java
private void updateWeeklyChart(List<DataManager.Expense> expenses) {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    
    long todayStart = cal.getTimeInMillis();
    List<String> labels = new ArrayList<>();
    List<Double> totals = new ArrayList<>();

    // Prepare 7 days (Mon, Tue, Wed, etc.)
    for (int i = 6; i >= 0; i--) {
        Calendar day = (Calendar) cal.clone();
        day.add(Calendar.DAY_OF_YEAR, -i);
        String label = new SimpleDateFormat("EEE", Locale.getDefault()).format(day.getTime());
        labels.add(label);
        totals.add(0.0);
    }

    // Sum expenses into day buckets
    for (DataManager.Expense expense : expenses) {
        Date d = parseExpenseDate(expense.date);
        if (d == null) continue;
        
        Calendar expCal = Calendar.getInstance();
        expCal.setTime(d);
        expCal.set(Calendar.HOUR_OF_DAY, 0);
        // ... calculate which day bucket this expense belongs to
        int index = (int) (6 - diffDays);
        totals.set(index, totals.get(index) + expense.amount);
    }

    weeklyChart.setData(labels, totals);
}
```

**What to Say:**
> "The weekly chart creates 7 day buckets representing the last 7 days. I initialize each bucket with zero, then loop through all expenses. For each expense, I parse its date and determine which day bucket it belongs to. I add the expense amount to that bucket's total. Finally, I pass the labels and totals to the custom chart view which draws the bars."

---

## 12. RecyclerView Adapter Pattern

**Location:** `ExpenseAdapter.java` - `onCreateViewHolder()` and `onBindViewHolder()`  
**Lines:** 43-117

**What it does:**
- Creates ViewHolder instances (reused)
- Binds expense data to views
- Sets up click listeners for edit/delete

**Key Code Block:**
```java
@Override
public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    // Inflate item layout - happens only a few times
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_expense, parent, false);
    return new ExpenseViewHolder(view);
}

@Override
public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
    // Get expense at this position
    DataManager.Expense expense = expenses.get(position);
    // Bind data to views
    holder.bind(expense);
}

class ExpenseViewHolder extends RecyclerView.ViewHolder {
    public void bind(DataManager.Expense expense) {
        // Set text values
        tvCategory.setText(expense.category);
        tvAmount.setText(String.format(Locale.getDefault(), "-$%.2f", expense.amount));
        tvNote.setText(expense.note);
        tvDate.setText(expense.date);
        
        // Set category icon
        tvCategoryIcon.setText(getCategoryIcon(expense.category));
        
        // Setup menu button for edit/delete
        btnMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.getMenu().add("Edit");
            popupMenu.getMenu().add("Delete");
            // ... handle clicks
        });
    }
}
```

**What to Say:**
> "RecyclerView uses the ViewHolder pattern for efficiency. `onCreateViewHolder()` creates ViewHolder instances by inflating the item layout - this happens only a few times as views are reused. `onBindViewHolder()` binds expense data to those views each time an item scrolls into view. The ViewHolder's `bind()` method sets all the text values and sets up click listeners. This reuses views instead of creating new ones, making scrolling smooth even with many items."

---

## 🎯 Quick Summary for Teacher

**Core Functionality Flow:**

1. **Add Expense:** User inputs → Validate → Check Budget → Save to Database
2. **Display Expenses:** Database → Filter → Sort → RecyclerView
3. **Edit Expense:** Pre-fill Dialog → Modify → Check Budget → Update Database
4. **Delete Expense:** Confirm → Delete from Database → Refresh List
5. **Budget Management:** Set Limit → Calculate Spent → Compare → Show Warning
6. **Analytics:** Calculate Totals → Group by Category → Calculate Percentages → Display Charts

**Key Concepts:**
- **CRUD Operations:** Create, Read, Update, Delete expenses
- **Data Filtering:** Search through multiple fields
- **Data Sorting:** Multiple sort criteria (date, amount, category)
- **Budget Tracking:** Real-time calculation and warnings
- **Data Aggregation:** Totals, percentages, category breakdowns
- **RecyclerView:** Efficient list display with ViewHolder pattern

---

## 📝 Practice Questions

1. **"How does adding an expense work?"**
   - Answer: Validate input → Check budget → Save to database → Navigate back

2. **"How does budget checking work?"**
   - Answer: Find budget → Calculate total spent → Add new expense → Compare with limit

3. **"How does search work?"**
   - Answer: Loop through expenses → Check if query matches note/category/amount/date → Return matches

4. **"How does RecyclerView improve performance?"**
   - Answer: ViewHolder pattern reuses views instead of creating new ones for each item

5. **"How are analytics calculated?"**
   - Answer: Loop through expenses → Calculate totals → Group by category → Calculate percentages

---

## 🔍 Files to Review

1. ✅ `DataManager.java` - Business logic (lines 277-458)
2. ✅ `HomeFragment.java` - Expense display & management (lines 147-253)
3. ✅ `AddExpenseFragment.java` - Add expense functionality (lines 421-480)
4. ✅ `BudgetFragment.java` - Budget management (lines 111-117)
5. ✅ `AnalyticsFragment.java` - Analytics calculation (lines 119-193)
6. ✅ `ExpenseAdapter.java` - List display (lines 43-117)
7. ✅ `DatabaseHelper.java` - Database operations (expense CRUD methods)

Good luck! 🎓
