package com.example.nzreceiptapp.domain.model;

import java.util.List;

/**
 * 消費明細品項 (Domain Entity)
 */
public class ReceiptItem {
    private final String id;
    private final String rawName;           // OCR 辨識出的原始名稱
    private final String cleanedName;       // 移除雜訊與單位後的標準名稱
    
    private final double quantity;          // 購買數量 (可以是件數 2.0，或是秤重重量 1.245)
    private final String unit;              // 商品特徵單位 (e.g., "L", "KG", "g", "PK", "EA")
    private final long unitPriceCents;      // 單價 (分)
    
    private final List<ItemDiscount> discounts; // 該品項掛載的單項折扣清單
    private final Category category;        // 綁定的二級分類
    private final boolean specialMk;        // 特價品標記 (保留先前需求)

    public ReceiptItem(String id, String rawName, String cleanedName, double quantity, 
                       String unit, long unitPriceCents, List<ItemDiscount> discounts, 
                       Category category, boolean specialMk) {
        this.id = id;
        this.rawName = rawName;
        this.cleanedName = cleanedName;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.discounts = discounts;
        this.category = category;
        this.specialMk = specialMk;
    }

    // 核心商業邏輯：計算該商品折扣前的原始小計
    public long getOriginalSubtotalCents() {
        return Math.round(quantity * unitPriceCents);
    }

    // 核心商業邏輯：計算該商品的實際應付小計 (原始總額 - 扣除所有單項折扣)
    public long getFinalSubtotalCents() {
        long totalItemDiscount = 0;
        if (discounts != null) {
            for (ItemDiscount discount : discounts) {
                totalItemDiscount += discount.getAmountCents();
            }
        }
        return getOriginalSubtotalCents() - totalItemDiscount;
    }

    // 判斷是否為生鮮秤重商品
    public boolean isWeightBased() {
        return "KG".equalsIgnoreCase(unit) || "g".equalsIgnoreCase(unit);
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getRawName() { return rawName; }
    public String getCleanedName() { return cleanedName; }
    public String getName() { return cleanedName; } // Alias for tests if needed
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public List<ItemDiscount> getDiscounts() { return discounts; }
    public Category getCategory() { return category; }
    public boolean getSpecialMk() { return specialMk; }
}
