package com.example.nzreceiptapp.data.local.entity;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * 用於 Room 查詢的嵌套關係類，獲取完整的收據結構
 */
public class ReceiptWithItems {
    @Embedded
    public ReceiptEntity receipt;

    @Relation(parentColumn = "store_id", entityColumn = "id")
    public StoreEntity store;

    @Relation(entity = ReceiptItemEntity.class, parentColumn = "id", entityColumn = "receipt_id")
    public List<ReceiptItemWithDiscounts> items;
}
