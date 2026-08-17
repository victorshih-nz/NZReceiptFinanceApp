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
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertStore(StoreEntity store);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReceipt(ReceiptEntity receipt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReceiptItems(List<ReceiptItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItemDiscounts(List<ItemDiscountEntity> discounts);

    @Transaction
    @Query("SELECT * FROM receipts ORDER BY purchase_date DESC LIMIT :limit OFFSET :offset")
    List<ReceiptWithItems> getReceiptsPaged(int limit, int offset);

    @Query("SELECT COUNT(*) FROM receipts")
    int countReceipts();

    @Transaction
    default PageData<ReceiptWithItems> getReceiptsPage(int requestedPage, int pageSize) {
        if (requestedPage < 1) {
            throw new IllegalArgumentException("requestedPage must be at least 1");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        int totalRecords = countReceipts();
        int totalPages = totalRecords == 0
                ? 1
                : ((totalRecords - 1) / pageSize) + 1;
        int effectivePage = Math.min(requestedPage, totalPages);
        int offset = (effectivePage - 1) * pageSize;
        List<ReceiptWithItems> rows = getReceiptsPaged(pageSize, offset);
        return new PageData<>(rows, effectivePage, totalRecords);
    }

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

    @Query("SELECT COUNT(*) FROM receipt_items")
    int countReceiptItems();

    @Transaction
    default PageData<ReceiptItemRow> getAllItemsPage(int requestedPage, int pageSize) {
        if (requestedPage < 1) {
            throw new IllegalArgumentException("requestedPage must be at least 1");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        int totalRecords = countReceiptItems();
        int totalPages = totalRecords == 0
                ? 1
                : ((totalRecords - 1) / pageSize) + 1;
        int effectivePage = Math.min(requestedPage, totalPages);
        int offset = (effectivePage - 1) * pageSize;
        List<ReceiptItemRow> rows = getAllItemsPaged(pageSize, offset);
        return new PageData<>(rows, effectivePage, totalRecords);
    }

    @Query("DELETE FROM receipts WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM stores WHERE chain_name = :chainName AND branch_name = :branchName LIMIT 1")
    StoreEntity findStore(String chainName, String branchName);

    @Query("SELECT store_id FROM receipts WHERE id = :receiptId LIMIT 1")
    String findStoreIdForReceipt(String receiptId);

    @Query("DELETE FROM stores WHERE id = :storeId AND NOT EXISTS "
            + "(SELECT 1 FROM receipts WHERE store_id = :storeId)")
    void deleteStoreIfUnused(String storeId);

    @Transaction
    default void saveFullReceipt(StoreEntity store, ReceiptEntity receipt, 
                                List<ReceiptItemEntity> items, List<ItemDiscountEntity> discounts) {
        StoreEntity existingStore = findStore(store.chainName, store.branchName);
        if (existingStore == null) {
            insertStore(store);
            existingStore = findStore(store.chainName, store.branchName);
        }
        if (existingStore == null) {
            throw new IllegalStateException("Unable to create or find store");
        }
        receipt.storeId = existingStore.id;
        insertReceipt(receipt);
        insertReceiptItems(items);
        if (discounts != null && !discounts.isEmpty()) {
            insertItemDiscounts(discounts);
        }
    }

    @Transaction
    default void deleteReceiptAndUnusedStore(String receiptId) {
        String storeId = findStoreIdForReceipt(receiptId);
        deleteById(receiptId);
        if (storeId != null) {
            deleteStoreIfUnused(storeId);
        }
    }

    final class PageData<T> {
        private final List<T> rows;
        private final int currentPage;
        private final int totalRecords;

        public PageData(List<T> rows, int currentPage, int totalRecords) {
            this.rows = rows;
            this.currentPage = currentPage;
            this.totalRecords = totalRecords;
        }

        public List<T> getRows() {
            return rows;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getTotalRecords() {
            return totalRecords;
        }
    }
}
