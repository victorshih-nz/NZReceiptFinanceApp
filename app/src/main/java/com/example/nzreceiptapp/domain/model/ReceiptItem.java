package com.example.nzreceiptapp.domain.model;

import java.util.UUID;

public class ReceiptItem {
    private String id;
    private String rawName;
    private String cleanName;
    private double quantity;
    private String unit;
    private long unitPriceCents;
    private long discountCents;
    private long totalPriceCents; // 用來存儲解析器直接抓到的總價

    private boolean specialMk;

    public ReceiptItem() {
        this.id = UUID.randomUUID().toString();
        this.unit = "ea";
        this.quantity = 1.0;
    }

    public ReceiptItem(String id, String rawName, String cleanName, double quantity, String unit,
                       long unitPriceCents, long discountCents) {
        this.id = id;
        this.rawName = rawName;
        this.cleanName = cleanName;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.discountCents = discountCents;
        this.totalPriceCents = Math.round(quantity * unitPriceCents);
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getRawName() { return rawName; }
    public String getName() { return rawName; } // Alias for tests and parsers
    public String getCleanName() { return cleanName; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public long getDiscountCents() { return discountCents; }

    public boolean getSpecialMk() { return specialMk; }

    public long getSubtotalCents() {
        // 如果有顯式設定的總價，優先回傳；否則計算 Qty * Price
        return (totalPriceCents > 0) ? totalPriceCents : Math.round(quantity * unitPriceCents);
    }

    // --- Setters (Standard) ---
    public void setId(String id) { this.id = id; }
    public void setRawName(String rawName) { this.rawName = rawName; }
    public void setCleanName(String cleanName) { this.cleanName = cleanName; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setUnitPriceCents(long unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    public void setDiscountCents(long discountCents) { this.discountCents = discountCents; }
    public void setTotalPriceCents(long totalPriceCents) { this.totalPriceCents = totalPriceCents; }
    public long getTotalPriceCents() { return totalPriceCents; }

    public void setSpecialMk(boolean specialMk) { this.specialMk = specialMk; }

    // --- Helper Setters for Parsers ---
    public void setName(String name) {
        this.rawName = name;
        this.cleanName = name;
    }

    public void setUnitPrice(double price) {
        this.unitPriceCents = Math.round(price * 100);
    }

    public void setTotalPrice(double price) {
        this.totalPriceCents = Math.round(price * 100);
    }
}
