package com.example.nzreceiptapp.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * 用於 Room 查詢的關係類，將商品與其折扣、分類綁定
 */
public class ReceiptItemWithDiscounts {
    @Embedded
    public ReceiptItemEntity item;

    @Relation(parentColumn = "id", entityColumn = "receipt_item_id")
    public List<ItemDiscountEntity> discounts;

    @Relation(parentColumn = "category_id", entityColumn = "id")
    public CategoryEntity category;
}
