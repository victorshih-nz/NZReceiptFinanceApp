package com.example.nzreceiptapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "receipt_items",
        foreignKeys = {
                @ForeignKey(entity = ReceiptEntity.class,
                        parentColumns = "id",
                        childColumns = "receipt_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = CategoryEntity.class,
                        parentColumns = "id",
                        childColumns = "category_id",
                        onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("receipt_id"), @Index("category_id")})
public class ReceiptItemEntity {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "receipt_id")
    @NonNull
    public String receiptId;

    @ColumnInfo(name = "raw_name")
    public String rawName;

    @ColumnInfo(name = "cleaned_name")
    public String cleanedName;

    @ColumnInfo(name = "quantity")
    public double quantity;

    @ColumnInfo(name = "unit")
    public String unit;

    @ColumnInfo(name = "unit_price_cents")
    public long unitPriceCents;

    @ColumnInfo(name = "category_id")
    public String categoryId;

    @ColumnInfo(name = "special_mk")
    public boolean specialMk;

    public ReceiptItemEntity(@NonNull String id, @NonNull String receiptId, String rawName, 
                             String cleanedName, double quantity, String unit, 
                             long unitPriceCents, String categoryId, boolean specialMk) {
        this.id = id;
        this.receiptId = receiptId;
        this.rawName = rawName;
        this.cleanedName = cleanedName;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.categoryId = categoryId;
        this.specialMk = specialMk;
    }
}
