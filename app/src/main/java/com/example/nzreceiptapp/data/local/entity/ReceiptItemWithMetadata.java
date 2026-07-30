package com.example.nzreceiptapp.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * 用於分頁查詢所有品項的關係類，包含收據與商店的基礎資訊
 */
public class ReceiptItemWithMetadata {
    @Embedded
    public ReceiptItemEntity item;

    @Relation(parentColumn = "receipt_id", entityColumn = "id")
    public ReceiptEntity receipt;

    @Relation(parentColumn = "id", entityColumn = "receipt_item_id")
    public List<ItemDiscountEntity> discounts;

    @Relation(parentColumn = "category_id", entityColumn = "id")
    public CategoryEntity category;
}
