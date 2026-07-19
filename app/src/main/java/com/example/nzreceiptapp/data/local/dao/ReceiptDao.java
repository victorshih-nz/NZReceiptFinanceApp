package com.example.nzreceiptapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Transaction;

import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;

import java.util.List;

@Dao
public interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStore(StoreEntity store);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReceipt(ReceiptEntity receipt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReceiptItems(List<ReceiptItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItemDiscounts(List<ItemDiscountEntity> discounts);

    @Transaction
    default void saveFullReceipt(StoreEntity store, ReceiptEntity receipt, 
                                List<ReceiptItemEntity> items, List<ItemDiscountEntity> discounts) {
        insertStore(store);
        insertReceipt(receipt);
        insertReceiptItems(items);
        if (discounts != null && !discounts.isEmpty()) {
            insertItemDiscounts(discounts);
        }
    }
}
