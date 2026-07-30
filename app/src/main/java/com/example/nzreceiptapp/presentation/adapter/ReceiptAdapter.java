package com.example.nzreceiptapp.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nzreceiptapp.databinding.ItemReceiptBinding;
import com.example.nzreceiptapp.domain.model.Receipt;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ViewHolder> {

    public interface OnReceiptClickListener {
        void onClick(Receipt receipt);
    }

    public interface OnDeleteClickListener {
        void onDelete(Receipt receipt);
    }

    private final List<Receipt> receipts = new ArrayList<>();
    private final OnDeleteClickListener deleteListener;
    private final OnReceiptClickListener clickListener;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ReceiptAdapter(OnReceiptClickListener clickListener, OnDeleteClickListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void submitList(List<Receipt> newList) {
        receipts.clear();
        if (newList != null) {
            receipts.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReceiptBinding binding = ItemReceiptBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Receipt receipt = receipts.get(position);
        holder.bind(receipt);
    }

    @Override
    public int getItemCount() {
        return receipts.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceiptBinding binding;

        ViewHolder(ItemReceiptBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Receipt receipt) {
            binding.txtChain.setText(receipt.getStore().getChainName());
            binding.txtBranch.setText(receipt.getStore().getBranchName());
            binding.txtDate.setText(receipt.getPurchaseDate().format(DATE_FORMATTER));
            
            int itemCount = receipt.getItems() != null ? receipt.getItems().size() : 0;
            binding.txtItemCount.setText(itemCount + " items");

            double totalAmount = receipt.getFinalPayableCents() / 100.0;
            binding.txtTotal.setText(String.format(Locale.getDefault(), "$%.2f", totalAmount));

            binding.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(receipt);
                }
            });

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onClick(receipt);
                }
            });
        }
    }
}
