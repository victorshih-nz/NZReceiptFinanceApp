package com.example.nzreceiptapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemRow;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
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
    @Query("SELECT * FROM receipts ORDER BY purchase_date DESC LIMIT :limit OFFSET :offset")
    List<ReceiptWithItems> getReceiptsPaged(int limit, int offset);

    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    ReceiptWithItems getReceiptById(String id);

    @Transaction
    @Query("SELECT ri.*, s.chain_name as chainName, s.branch_name as branchName, r.purchase_date as purchaseDate " +
           "FROM receipt_items ri " +
           "JOIN receipts r ON ri.receipt_id = r.id " +
           "JOIN stores s ON r.store_id = s.id " +
           "ORDER BY r.purchase_date DESC LIMIT :limit OFFSET :offset")
    List<ReceiptItemRow> getAllItemsPaged(int limit, int offset);

    @Query("DELETE FROM receipts WHERE id = :id")
    void deleteById(String id);

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
