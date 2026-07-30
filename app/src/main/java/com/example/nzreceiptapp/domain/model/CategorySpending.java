package com.example.nzreceiptapp.domain.model;

/**
 * 用於分析統計的資料模型，表示特定分類的消費總額
 */
public class CategorySpending {
    private final String categoryName;
    private final long totalAmountCents;

    public CategorySpending(String categoryName, long totalAmountCents) {
        this.categoryName = categoryName;
        this.totalAmountCents = totalAmountCents;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public long getTotalAmountCents() {
        return totalAmountCents;
    }
}
