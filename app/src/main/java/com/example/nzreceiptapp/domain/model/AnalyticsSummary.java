package com.example.nzreceiptapp.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, cents-based result for one selected period. */
public final class AnalyticsSummary {
    private final long totalSpendingCents;
    private final long categoryTotalCents;
    private final int receiptCount;
    private final int itemCount;
    private final List<CategorySpending> categories;

    public AnalyticsSummary(long totalSpendingCents, long categoryTotalCents,
                            int receiptCount, int itemCount, List<CategorySpending> categories) {
        this.totalSpendingCents = totalSpendingCents;
        this.categoryTotalCents = categoryTotalCents;
        this.receiptCount = receiptCount;
        this.itemCount = itemCount;
        this.categories = Collections.unmodifiableList(new ArrayList<>(categories));
    }

    /** Sum of Receipt.getFinalPayableCents(); receipt-level discounts included. */
    public long getTotalSpendingCents() { return totalSpendingCents; }
    /** Sum of item final subtotals; receipt-level discounts NOT allocated. */
    public long getCategoryTotalCents() { return categoryTotalCents; }
    public int getReceiptCount() { return receiptCount; }
    public int getItemCount() { return itemCount; }
    public List<CategorySpending> getCategories() { return categories; }
    /** Informational difference, not necessarily an error (e.g., receipt-level discounts). */
    public long getCategoryMinusPaidCents() { return categoryTotalCents - totalSpendingCents; }
    public boolean isEmpty() { return receiptCount == 0; }
}
