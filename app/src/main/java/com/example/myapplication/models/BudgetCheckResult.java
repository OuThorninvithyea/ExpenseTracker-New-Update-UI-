package com.example.myapplication.models;

public class BudgetCheckResult {
    public boolean exceedsBudget;
    public double budgetLimit;
    public double currentSpent;
    public double newTotal;

    public BudgetCheckResult(boolean exceedsBudget, double budgetLimit, double currentSpent, double newTotal) {
        this.exceedsBudget = exceedsBudget;
        this.budgetLimit = budgetLimit;
        this.currentSpent = currentSpent;
        this.newTotal = newTotal;
    }
}
