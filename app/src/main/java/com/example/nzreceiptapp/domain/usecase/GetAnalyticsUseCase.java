package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.CategorySpending;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 統計消費數據的 Use Case
 */
public class GetAnalyticsUseCase {
    private final IReceiptRepository repository;

    public GetAnalyticsUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public List<CategorySpending> execute() {
        List<Receipt> receipts = repository.getAllReceipts();
        Map<String, Long> spendingMap = new HashMap<>();

        for (Receipt receipt : receipts) {
            for (ReceiptItem item : receipt.getItems()) {
                String categoryName = (item.getCategory() != null) ? item.getCategory().getName() : "Uncategorized";
                long amount = item.getFinalSubtotalCents();
                spendingMap.put(categoryName, spendingMap.getOrDefault(categoryName, 0L) + amount);
            }
        }

        List<CategorySpending> results = new ArrayList<>();
        for (Map.Entry<String, Long> entry : spendingMap.entrySet()) {
            results.add(new CategorySpending(entry.getKey(), entry.getValue()));
        }

        return results;
    }
}
