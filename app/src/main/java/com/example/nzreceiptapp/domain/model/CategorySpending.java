package com.example.nzreceiptapp.domain.model;

/** Immutable spending of one stable category ID (or the uncategorized bucket). */
public final class CategorySpending {
    private final String categoryId;
    private final String categoryName;
    private final long totalAmountCents;

    /** Legacy constructor retained for existing callers. */
    public CategorySpending(String categoryName, long totalAmountCents) {
        this(null, categoryName, totalAmountCents);
    }

    public CategorySpending(String categoryId, String categoryName, long totalAmountCents) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalAmountCents = totalAmountCents;
    }

    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public long getTotalAmountCents() { return totalAmountCents; }
}
