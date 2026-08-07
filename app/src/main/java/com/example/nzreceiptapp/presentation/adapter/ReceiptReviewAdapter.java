package com.example.nzreceiptapp.presentation.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nzreceiptapp.databinding.ItemReceiptReviewBinding;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.ReceiptItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Editable presentation model for the review screen. */
public final class ReceiptReviewAdapter
        extends RecyclerView.Adapter<ReceiptReviewAdapter.ViewHolder> {
    private final List<EditableItem> items = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private final Runnable onItemsChanged;

    public ReceiptReviewAdapter(Runnable onItemsChanged) {
        this.onItemsChanged = onItemsChanged;
    }

    public void submit(List<ReceiptItem> receiptItems, List<Category> availableCategories) {
        items.clear();
        categories.clear();
        if (availableCategories != null) categories.addAll(availableCategories);
        if (receiptItems != null) {
            for (ReceiptItem item : receiptItems) items.add(new EditableItem(item));
        }
        notifyDataSetChanged();
    }

    public List<ReceiptItem> buildReceiptItems() {
        List<ReceiptItem> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            result.add(items.get(index).toDomain(index + 1));
        }
        return result;
    }

    public void addEmptyItem() {
        ReceiptItem item = new ReceiptItem(
                UUID.randomUUID().toString(), "", "", 1.0, "ea", 0,
                Collections.emptyList(), null, false);
        items.add(new EditableItem(item));
        notifyItemInserted(items.size() - 1);
        notifyItemsChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReceiptReviewBinding binding = ItemReceiptReviewBinding.inflate(
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

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceiptReviewBinding binding;
        private TextWatcher nameWatcher;
        private TextWatcher quantityWatcher;
        private TextWatcher unitWatcher;
        private TextWatcher priceWatcher;
        private boolean bindingCategory;

        ViewHolder(ItemReceiptReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EditableItem item) {
            detachWatchers();
            binding.editItemName.setText(item.name);
            binding.editQuantity.setText(item.quantity);
            binding.editUnit.setText(item.unit);
            binding.editUnitPrice.setText(item.unitPrice);

            List<String> labels = new ArrayList<>();
            labels.add("Uncategorized");
            for (Category category : categories) {
                String parent = category.getParentCategory() == null
                        ? ""
                        : category.getParentCategory().getName() + " / ";
                labels.add(parent + category.getName());
            }
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                    binding.getRoot().getContext(),
                    android.R.layout.simple_spinner_item,
                    labels);
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCategory.setAdapter(categoryAdapter);

            bindingCategory = true;
            binding.spinnerCategory.setSelection(findCategoryPosition(item.category));
            bindingCategory = false;
            binding.spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (bindingCategory) return;
                    item.category = position == 0 ? null : categories.get(position - 1);
                    notifyItemsChanged();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    item.category = null;
                }
            });

            nameWatcher = watcher(value -> item.name = value);
            quantityWatcher = watcher(value -> item.quantity = value);
            unitWatcher = watcher(value -> item.unit = value);
            priceWatcher = watcher(value -> item.unitPrice = value);
            binding.editItemName.addTextChangedListener(nameWatcher);
            binding.editQuantity.addTextChangedListener(quantityWatcher);
            binding.editUnit.addTextChangedListener(unitWatcher);
            binding.editUnitPrice.addTextChangedListener(priceWatcher);
            binding.btnRemoveItem.setOnClickListener(ignored -> {
                int index = items.indexOf(item);
                if (index >= 0) {
                    items.remove(index);
                    notifyItemRemoved(index);
                    notifyItemRangeChanged(index, items.size() - index);
                    notifyItemsChanged();
                }
            });
        }

        private int findCategoryPosition(Category selected) {
            if (selected == null) return 0;
            for (int index = 0; index < categories.size(); index++) {
                if (categories.get(index).getId().equals(selected.getId())) return index + 1;
            }
            return 0;
        }

        private TextWatcher watcher(ValueConsumer consumer) {
            return new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    consumer.accept(s.toString());
                    notifyItemsChanged();
                }
                @Override public void afterTextChanged(Editable s) { }
            };
        }

        private void detachWatchers() {
            if (nameWatcher != null) binding.editItemName.removeTextChangedListener(nameWatcher);
            if (quantityWatcher != null) binding.editQuantity.removeTextChangedListener(quantityWatcher);
            if (unitWatcher != null) binding.editUnit.removeTextChangedListener(unitWatcher);
            if (priceWatcher != null) binding.editUnitPrice.removeTextChangedListener(priceWatcher);
        }
    }

    private void notifyItemsChanged() {
        if (onItemsChanged != null) onItemsChanged.run();
    }

    private interface ValueConsumer {
        void accept(String value);
    }

    private static final class EditableItem {
        private final ReceiptItem original;
        private String name;
        private String quantity;
        private String unit;
        private String unitPrice;
        private Category category;

        EditableItem(ReceiptItem item) {
            original = item;
            name = item.getCleanedName();
            quantity = Double.toString(item.getQuantity());
            unit = item.getUnit();
            unitPrice = String.format(Locale.US, "%.2f", item.getUnitPriceCents() / 100.0);
            category = item.getCategory();
        }

        ReceiptItem toDomain(int rowNumber) {
            String cleanName = name == null ? "" : name.trim();
            if (cleanName.isEmpty()) {
                throw new IllegalArgumentException("Item " + rowNumber + " needs a name");
            }
            double parsedQuantity;
            long parsedUnitPrice;
            try {
                parsedQuantity = Double.parseDouble(quantity.trim());
                parsedUnitPrice = Math.round(Double.parseDouble(unitPrice.trim()) * 100);
            } catch (Exception exception) {
                throw new IllegalArgumentException(
                        "Item " + rowNumber + " has an invalid quantity or price");
            }
            if (parsedQuantity <= 0 || parsedUnitPrice < 0) {
                throw new IllegalArgumentException(
                        "Item " + rowNumber + " quantity must be positive and price cannot be negative");
            }
            String cleanUnit = unit == null || unit.trim().isEmpty() ? "ea" : unit.trim();
            return new ReceiptItem(
                    original.getId(),
                    original.getRawName(),
                    cleanName,
                    parsedQuantity,
                    cleanUnit,
                    parsedUnitPrice,
                    original.getDiscounts() == null
                            ? Collections.emptyList()
                            : original.getDiscounts(),
                    category,
                    original.getSpecialMk());
        }
    }
}
