package com.example.myapplication.services;

import android.content.Context;
import com.example.myapplication.data.repositories.BudgetRepository;
import com.example.myapplication.data.repositories.ExpenseRepository;
import com.example.myapplication.models.Budget;
import com.example.myapplication.models.BudgetCheckResult;
import com.example.myapplication.models.Expense;
import java.util.List;

public class ExpenseService {
    private ExpenseRepository expenseRepository;
    private BudgetRepository budgetRepository;

    public ExpenseService(Context context) {
        this.expenseRepository = new ExpenseRepository(context);
        this.budgetRepository = new BudgetRepository(context);
    }

    public long addExpense(String category, double amount, String note, String date) {
        return expenseRepository.addExpense(category, amount, note, date);
    }

    public List<Expense> getExpenses() {
        return expenseRepository.getExpenses();
    }

    public boolean updateExpense(int expenseId, String category, double amount, String note, String date) {
        return expenseRepository.updateExpense(expenseId, category, amount, note, date);
    }

    public boolean deleteExpense(int expenseId) {
        return expenseRepository.deleteExpense(expenseId);
    }

    public boolean clearExpenses() {
        return expenseRepository.clearExpenses();
    }

    public BudgetCheckResult checkBudget(String category, double amount) {
        List<Budget> budgets = budgetRepository.getBudgets();
        Budget targetBudget = null;
        for (Budget b : budgets) {
            if (b.category.equals(category)) {
                targetBudget = b;
                break;
            }
        }

        if (targetBudget == null) {
            return new BudgetCheckResult(false, 0, 0, 0);
        }

        List<Expense> expenses = expenseRepository.getExpenses();
        double totalSpent = 0;
        for (Expense e : expenses) {
            if (e.category.equals(category)) {
                totalSpent += e.amount;
            }
        }

        double newTotal = totalSpent + amount;
        boolean exceeds = newTotal >= targetBudget.limit;

        return new BudgetCheckResult(exceeds, targetBudget.limit, totalSpent, newTotal);
    }
    
    public BudgetCheckResult checkBudgetOnUpdate(String category, double newAmount, int expenseId) {
        List<Budget> budgets = budgetRepository.getBudgets();
        Budget targetBudget = null;
        for (Budget b : budgets) {
            if (b.category.equals(category)) {
                targetBudget = b;
                break;
            }
        }

        if (targetBudget == null) {
            return new BudgetCheckResult(false, 0, 0, 0);
        }

        List<Expense> expenses = expenseRepository.getExpenses();
        double totalSpent = 0;
        for (Expense e : expenses) {
            if (e.category.equals(category) && e.id != expenseId) {
                totalSpent += e.amount;
            }
        }

        double newTotal = totalSpent + newAmount;
        boolean exceeds = newTotal >= targetBudget.limit;

        return new BudgetCheckResult(exceeds, targetBudget.limit, totalSpent, newTotal);
    }
}
