package com.example.nzreceiptapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(tableName = "receipts",
        foreignKeys = @ForeignKey(entity = StoreEntity.class,
                parentColumns = "id",
                childColumns = "store_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("store_id")})
public class ReceiptEntity {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "store_id")
    @NonNull
    public String storeId;

    @ColumnInfo(name = "purchase_date")
    public LocalDateTime purchaseDate;

    @ColumnInfo(name = "total_discount_cents")
    public long totalDiscountCents;

    @ColumnInfo(name = "is_synced")
    public boolean isSynced;

    public ReceiptEntity(@NonNull String id, @NonNull String storeId, LocalDateTime purchaseDate, 
                         long totalDiscountCents, boolean isSynced) {
        this.id = id;
        this.storeId = storeId;
        this.purchaseDate = purchaseDate;
        this.totalDiscountCents = totalDiscountCents;
        this.isSynced = isSynced;
    }
}
