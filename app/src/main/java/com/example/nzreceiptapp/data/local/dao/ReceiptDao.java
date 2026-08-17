package com.example.nzreceiptapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemRow;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;

import java.time.LocalDateTime;
import java.util.List;

@Dao
public interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertStore(StoreEntity store);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertReceipt(ReceiptEntity receipt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReceiptItems(List<ReceiptItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertItemDiscounts(List<ItemDiscountEntity> discounts);

    @Update
    int updateReceiptRow(ReceiptEntity receipt);

    @Transaction
    @Query("SELECT * FROM receipts "
            + "ORDER BY purchase_date DESC, saved_sequence ASC "
            + "LIMIT :limit OFFSET :offset")
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

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    ReceiptEntity getReceiptEntityById(String id);

    @Transaction
    @Query("SELECT receipts.* FROM receipts "
            + "INNER JOIN stores ON stores.id = receipts.store_id "
            + "WHERE stores.normalized_chain = :normalizedChain "
            + "AND purchase_date >= :hourStart AND purchase_date < :hourEnd "
            + "ORDER BY purchase_date ASC, saved_sequence ASC")
    List<ReceiptWithItems> getReceiptsInPurchaseHour(String normalizedChain,
                                                     LocalDateTime hourStart,
                                                     LocalDateTime hourEnd);

    @Transaction
    @Query("SELECT ri.*, s.chain_name as chainName, s.branch_name as branchName, r.purchase_date as purchaseDate " +
           "FROM receipt_items ri " +
           "JOIN receipts r ON ri.receipt_id = r.id " +
           "JOIN stores s ON r.store_id = s.id " +
           "ORDER BY r.purchase_date DESC, r.saved_sequence ASC, ri.rowid ASC "
           + "LIMIT :limit OFFSET :offset")
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

    @Query("DELETE FROM receipt_items WHERE receipt_id = :receiptId")
    void deleteItemsByReceiptId(String receiptId);

    @Query("SELECT * FROM stores WHERE normalized_chain = :normalizedChain "
            + "AND normalized_branch = :normalizedBranch LIMIT 1")
    StoreEntity findStore(String normalizedChain, String normalizedBranch);

    @Query("SELECT COALESCE(MAX(saved_sequence), 0) + 1 FROM receipts")
    long getNextSavedSequence();

    @Query("SELECT store_id FROM receipts WHERE id = :receiptId LIMIT 1")
    String findStoreIdForReceipt(String receiptId);

    @Query("DELETE FROM stores WHERE id = :storeId AND NOT EXISTS "
            + "(SELECT 1 FROM receipts WHERE store_id = :storeId)")
    void deleteStoreIfUnused(String storeId);

    @Transaction
    default void saveFullReceipt(StoreEntity store, ReceiptEntity receipt, 
                                List<ReceiptItemEntity> items, List<ItemDiscountEntity> discounts) {
        StoreEntity existingStore = findStore(
                store.normalizedChain, store.normalizedBranch);
        if (existingStore == null) {
            insertStore(store);
            existingStore = findStore(
                    store.normalizedChain, store.normalizedBranch);
        }
        if (existingStore == null) {
            throw new IllegalStateException("Unable to create or find store");
        }
        receipt.storeId = existingStore.id;
        receipt.savedSequence = getNextSavedSequence();
        insertReceipt(receipt);
        insertReceiptItems(items);
        if (discounts != null && !discounts.isEmpty()) {
            insertItemDiscounts(discounts);
        }
    }

    @Transaction
    default void updateFullReceipt(StoreEntity store, ReceiptEntity receipt,
                                   List<ReceiptItemEntity> items,
                                   List<ItemDiscountEntity> discounts) {
        ReceiptEntity existingReceipt = getReceiptEntityById(receipt.id);
        if (existingReceipt == null) {
            throw new IllegalArgumentException(
                    "Receipt does not exist: " + receipt.id);
        }

        StoreEntity targetStore = findStore(
                store.normalizedChain, store.normalizedBranch);
        if (targetStore == null) {
            insertStore(store);
            targetStore = findStore(
                    store.normalizedChain, store.normalizedBranch);
        }
        if (targetStore == null) {
            throw new IllegalStateException("Unable to create or find store");
        }

        String oldStoreId = existingReceipt.storeId;
        receipt.storeId = targetStore.id;
        receipt.savedSequence = existingReceipt.savedSequence;
        if (updateReceiptRow(receipt) != 1) {
            throw new IllegalStateException(
                    "Unable to update receipt: " + receipt.id);
        }

        deleteItemsByReceiptId(receipt.id);
        insertReceiptItems(items);
        if (discounts != null && !discounts.isEmpty()) {
            insertItemDiscounts(discounts);
        }
        deleteStoreIfUnused(oldStoreId);
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
