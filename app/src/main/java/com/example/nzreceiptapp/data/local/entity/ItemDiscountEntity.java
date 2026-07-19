package com.example.nzreceiptapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.nzreceiptapp.domain.model.ItemDiscount;

@Entity(tableName = "item_discounts",
        foreignKeys = @ForeignKey(entity = ReceiptItemEntity.class,
                parentColumns = "id",
                childColumns = "receipt_item_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("receipt_item_id")})
public class ItemDiscountEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "receipt_item_id")
    @NonNull
    public String receiptItemId;

    @ColumnInfo(name = "type")
    public ItemDiscount.DiscountType type;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "amount_cents")
    public long amountCents;

    public ItemDiscountEntity(@NonNull String receiptItemId, ItemDiscount.DiscountType type, 
                              String description, long amountCents) {
        this.receiptItemId = receiptItemId;
        this.type = type;
        this.description = description;
        this.amountCents = amountCents;
    }
}
