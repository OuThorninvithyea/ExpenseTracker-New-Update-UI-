package com.example.myapplication.models;

public class Expense {
    public int id;
    public String category;
    public double amount;
    public String note;
    public String date;

    public Expense(int id, String category, double amount, String note, String date) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.date = date;
    }
}
