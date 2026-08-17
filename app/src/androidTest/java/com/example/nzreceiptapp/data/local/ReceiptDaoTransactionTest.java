package com.example.nzreceiptapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.ItemDiscount;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class ReceiptDaoTransactionTest {
    private AppDatabase database;
    private ReceiptDao receiptDao;

    @Before
    public void setUp() {
        database = Room.inMemoryDatabaseBuilder(
                        InstrumentationRegistry.getInstrumentation()
                                .getTargetContext(),
                        AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        receiptDao = database.receiptDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void updateFullReceipt_replacesGraphAndPreservesSequence() {
        saveOriginalReceipt();
        long originalSequence = receiptDao
                .getReceiptEntityById("receipt-1").savedSequence;
        StoreEntity newStore = new StoreEntity(
                "new-store", "PAK'nSAVE", "Albany");
        ReceiptEntity edited = receiptEntity("new-store");
        ReceiptItemEntity newItem = new ReceiptItemEntity(
                "new-item", "receipt-1", "Bread", "Bread",
                1, "ea", 499, null, false);
        ItemDiscountEntity discount = new ItemDiscountEntity(
                "new-item", ItemDiscount.DiscountType.MEMBER_SAVING,
                "Club deal", 100);

        receiptDao.updateFullReceipt(
                newStore,
                edited,
                Collections.singletonList(newItem),
                Collections.singletonList(discount));

        ReceiptWithItems stored = receiptDao.getReceiptById("receipt-1");
        assertEquals("new-store", stored.receipt.storeId);
        assertEquals(originalSequence, stored.receipt.savedSequence);
        assertEquals(1, stored.items.size());
        assertEquals("new-item", stored.items.get(0).item.id);
        assertEquals(1, stored.items.get(0).discounts.size());
        assertNull(receiptDao.findStore("woolworths", "albany"));
    }

    @Test
    public void updateFullReceipt_itemFailureRollsBackWholeGraph() {
        saveOriginalReceipt();
        StoreEntity newStore = new StoreEntity(
                "new-store", "PAK'nSAVE", "Albany");
        ReceiptEntity edited = receiptEntity("new-store");
        ReceiptItemEntity invalidItem = new ReceiptItemEntity(
                "invalid-item", "receipt-1", "Bread", "Bread",
                1, "ea", 499, "missing-category", false);

        try {
            receiptDao.updateFullReceipt(
                    newStore,
                    edited,
                    Collections.singletonList(invalidItem),
                    Collections.emptyList());
            fail("Expected the foreign-key violation to roll back the update");
        } catch (RuntimeException expected) {
            // The transaction must restore every write made before this failure.
        }

        ReceiptWithItems stored = receiptDao.getReceiptById("receipt-1");
        assertEquals("old-store", stored.receipt.storeId);
        assertEquals(1, stored.items.size());
        assertEquals("old-item", stored.items.get(0).item.id);
        assertNull(receiptDao.findStore("paknsave", "albany"));
    }

    private void saveOriginalReceipt() {
        StoreEntity store = new StoreEntity(
                "old-store", "Woolworths", "Albany");
        ReceiptItemEntity item = new ReceiptItemEntity(
                "old-item", "receipt-1", "Milk", "Milk",
                1, "ea", 399, null, false);
        receiptDao.saveFullReceipt(
                store,
                receiptEntity(store.id),
                Collections.singletonList(item),
                Collections.emptyList());
    }

    private ReceiptEntity receiptEntity(String storeId) {
        return new ReceiptEntity(
                "receipt-1", storeId,
                LocalDateTime.of(2026, 8, 17, 10, 30),
                0, false, "OCR", "image.jpg", 399L);
    }
}
