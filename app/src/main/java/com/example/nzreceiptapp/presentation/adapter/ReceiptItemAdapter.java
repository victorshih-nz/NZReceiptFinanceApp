package com.example.nzreceiptapp.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nzreceiptapp.databinding.ItemReceiptItemSummaryBinding;
import com.example.nzreceiptapp.domain.model.ReceiptItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptItemAdapter extends RecyclerView.Adapter<ReceiptItemAdapter.ViewHolder> {

    private final List<ReceiptItem> items = new ArrayList<>();

    public void submitList(List<ReceiptItem> newList) {
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

        void bind(ReceiptItem item) {
            binding.txtName.setText(item.getName());
            
            String qtyInfo = item.getQuantity() + " " + item.getUnit();
            binding.txtMetadata.setText(qtyInfo);
            
            String category = (item.getCategory() != null) ? item.getCategory().getName() : "Uncategorized";
            binding.txtCategory.setText(category);

            double amount = item.getFinalSubtotalCents() / 100.0;
            binding.txtAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount));
        }
    }
}
