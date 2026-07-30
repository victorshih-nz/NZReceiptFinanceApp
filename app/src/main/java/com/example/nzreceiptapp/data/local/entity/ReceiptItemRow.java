package com.example.nzreceiptapp.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 展平後的品項行，包含收據元數據
 */
public class ReceiptItemRow {
    @Embedded
    public ReceiptItemEntity item;

    public String chainName;
    public String branchName;
    public LocalDateTime purchaseDate;

    @Relation(parentColumn = "id", entityColumn = "receipt_item_id")
    public List<ItemDiscountEntity> discounts;

    @Relation(parentColumn = "category_id", entityColumn = "id")
    public CategoryEntity category;
}
