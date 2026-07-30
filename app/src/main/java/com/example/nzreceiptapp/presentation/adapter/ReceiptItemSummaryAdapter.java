package com.example.nzreceiptapp.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nzreceiptapp.databinding.ItemReceiptItemSummaryBinding;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptItemSummaryAdapter extends RecyclerView.Adapter<ReceiptItemSummaryAdapter.ViewHolder> {

    private final List<ReceiptItemSummary> items = new ArrayList<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    public void submitList(List<ReceiptItemSummary> newList) {
        items.clear();
        if (newList != null) {
            items.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReceiptItemSummaryBinding binding = ItemReceiptItemSummaryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceiptItemSummaryBinding binding;

        ViewHolder(ItemReceiptItemSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReceiptItemSummary summary) {
            binding.txtName.setText(summary.getItem().getName());
            binding.txtMetadata.setText(summary.getChainName() + " - " + summary.getPurchaseDate().format(DATE_FORMATTER));
            
            String category = (summary.getItem().getCategory() != null) ? summary.getItem().getCategory().getName() : "Uncategorized";
            binding.txtCategory.setText(category);

            double amount = summary.getItem().getFinalSubtotalCents() / 100.0;
            binding.txtAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount));
        }
    }
}
