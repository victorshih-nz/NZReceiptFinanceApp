package com.example.nzreceiptapp.domain.model;

public class ReceiptItem {
    private final String id;
    private final String rawName;
    private final String cleanName;
    private final double quantity;
    private final String unit;
    private final long unitPriceCents;
    private final long discountCents;

    public ReceiptItem(String id, String rawName, String cleanName, double quantity, String unit,
                       long unitPriceCents, long discountCents) {
        this.id = id;
        this.rawName = rawName;
        this.cleanName = cleanName;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.discountCents = discountCents;
    }

    public String getId() {
        return id;
    }

    public String getRawName() {
        return rawName;
    }

    public String getCleanName() {
        return cleanName;
    }

    public double getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public long getUnitPriceCents() {
        return unitPriceCents;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public long getSubtotalCents() {
        return Math.round(quantity * unitPriceCents);
    }
}
