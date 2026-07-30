package com.example.nzreceiptapp.domain.model;

import java.time.LocalDateTime;

/**
 * 用於「所有消費明細」視圖的包裝類，結合了品項與收據元數據
 */
public class ReceiptItemSummary {
    private final ReceiptItem item;
    private final String chainName;
    private final String branchName;
    private final LocalDateTime purchaseDate;

    public ReceiptItemSummary(ReceiptItem item, String chainName, String branchName, LocalDateTime purchaseDate) {
        this.item = item;
        this.chainName = chainName;
        this.branchName = branchName;
        this.purchaseDate = purchaseDate;
    }

    public ReceiptItem getItem() { return item; }
    public String getChainName() { return chainName; }
    public String getBranchName() { return branchName; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
}
