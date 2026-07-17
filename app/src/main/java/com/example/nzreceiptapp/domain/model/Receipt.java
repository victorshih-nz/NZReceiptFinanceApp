package com.example.nzreceiptapp.domain.model;

import java.util.List;

/**
 * 核心業務模型：整張消費收據
 */
public class Receipt {
    private String id;
    private String storeName;       // 賣場名稱 (例如: Woolworths, PAK'nSAVE)
    private String transactionDate; // 交易日期 (暫時用字串儲存，如 "2026-03-30")
    private List<ReceiptItem> items;// 這張收據包含的所有品項
    private long totalAmountCents;  // 收據上的總金額（分）

    public Receipt(){
        this.id = null;
        this.storeName = null;
        this.transactionDate = null;
        this.items = null;
        this.totalAmountCents = 0;
    }
    public Receipt(String id, String storeName, String transactionDate, List<ReceiptItem> items, long totalAmountCents) {
        this.id = id;
        this.storeName = storeName;
        this.transactionDate = transactionDate;
        this.items = items;
        this.totalAmountCents = totalAmountCents;
    }

    // 計算所有品項加總後的實付金額，用來跟收據總金額比對，驗收解析是否正確
    public long calculateCalculatedTotalCents() {
        long sum = 0;
        for (ReceiptItem item : items) {
            sum += item.getSubtotalCents();
        }
        return sum;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getStoreName() { return storeName; }
    public String getTransactionDate() { return transactionDate; }
    public List<ReceiptItem> getItems() { return items; }
    public long getTotalAmountCents() { return totalAmountCents; }

    // --- Setters ---
    public void setId(String id){
        this.id = id;
    }
    public void setStoreName(String storeName){
        this.storeName = storeName;
    }
    public void setTransactionDate(String transactionDate){
        this.transactionDate = transactionDate;
    }
    public void setTotalAmountCents(long ttlCents){
        this.totalAmountCents = ttlCents;
    }
}