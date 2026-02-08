package com.example.myapplication.adapters;

import com.example.myapplication.R;
import com.example.myapplication.models.Expense;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.utils.CurrencyHelper;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {
    private List<Expense> expenses;
    private OnExpenseClickListener listener;

    public interface OnExpenseClickListener {
        void onEditClick(Expense expense);    
        void onDeleteClick(Expense expense);  
    }

    public ExpenseAdapter(List<Expense> expenses, OnExpenseClickListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        
        Expense expense = expenses.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        
        notifyDataSetChanged();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCategory, tvNote, tvAmount, tvCategoryIcon, tvDate;
        private android.widget.ImageButton btnMenu;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvCategoryIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }

        public void bind(Expense expense) {
            
            tvCategory.setText(expense.category);
            
            tvNote.setText(expense.note);
            
            tvAmount.setText("-" + CurrencyHelper.formatCurrency(itemView.getContext(), expense.amount));

            if (expense.date != null && !expense.date.isEmpty()) {
                tvDate.setText(expense.date);
            } else {
                tvDate.setText("Today");
            }

            String icon = getCategoryIcon(expense.category);
            tvCategoryIcon.setText(icon);

            btnMenu.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.getMenu().add("Edit");
                popupMenu.getMenu().add("Delete");

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (listener != null) {
                            
                            if (item.getTitle().toString().equals("Edit")) {
                                listener.onEditClick(expense);
                            } else if (item.getTitle().toString().equals("Delete")) {
                                listener.onDeleteClick(expense);
                            }
                        }
                        return true;
                    }
                });

                popupMenu.show();
            });
        }

        private String getCategoryIcon(String category) {
            switch (category) {
                case "Food": return "🍔";
                case "Transport": return "🚗";
                case "Shopping": return "🛍️";
                case "Bills": return "📜";
                case "Entertainment": return "🍿";
                case "Others": return "✨";
                default: return "📦";
            }
        }
    }
}
