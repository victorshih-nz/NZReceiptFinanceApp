package com.example.nzreceiptapp.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 發票明細主體 (Domain Entity)
 */
public class Receipt {
    private final String id;
    private final Store store;                   // 聚合分店實體
    private final List<ReceiptItem> items;       // 一對多商品明細清單
    private final LocalDateTime purchaseDate;    // 發票消費時間
    private final long totalDiscountCents;       // 整張發票的額外折扣
    private final boolean isSynced;              // 雲端同步旗標
    private final String rawOcrText;              // 原始 OCR 文字，供日後重新解析/查錯
    private final String imageUri;                // 原始圖片來源（cache path 或 content URI）
    private final Long printedTotalCents;         // 收據上印出的總額；無法辨識時為 null

    public Receipt(String id, Store store, List<ReceiptItem> items, 
                   LocalDateTime purchaseDate, long totalDiscountCents, boolean isSynced) {
        this(id, store, items, purchaseDate, totalDiscountCents, isSynced,
                null, null, null);
    }

    public Receipt(String id, Store store, List<ReceiptItem> items,
                   LocalDateTime purchaseDate, long totalDiscountCents, boolean isSynced,
                   String rawOcrText, String imageUri, Long printedTotalCents) {
        this.id = id;
        this.store = store;
        this.items = items;
        this.purchaseDate = purchaseDate;
        this.totalDiscountCents = totalDiscountCents;
        this.isSynced = isSynced;
        this.rawOcrText = rawOcrText;
        this.imageUri = imageUri;
        this.printedTotalCents = printedTotalCents;
    }

    // 核心商業邏輯：計算整張發票「折扣前」的物質總金額
    public long getOriginalTotalCents() {
        long total = 0;
        if (items != null) {
            for (ReceiptItem item : items) {
                total += item.getOriginalSubtotalCents();
            }
        }
        return total;
    }

    // 核心商業邏輯：計算整張發票「實際支付」總額
    public long getFinalPayableCents() {
        long itemsTotal = 0;
        if (items != null) {
            for (ReceiptItem item : items) {
                itemsTotal += item.getFinalSubtotalCents();
            }
        }
        return itemsTotal - totalDiscountCents;
    }

    // --- Getters ---
    public String getId() { return id; }
    public Store getStore() { return store; }
    public List<ReceiptItem> getItems() { return items; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public long getTotalDiscountCents() { return totalDiscountCents; }
    public boolean isSynced() { return isSynced; }
    public String getRawOcrText() { return rawOcrText; }
    public String getImageUri() { return imageUri; }
    public Long getPrintedTotalCents() { return printedTotalCents; }
}
