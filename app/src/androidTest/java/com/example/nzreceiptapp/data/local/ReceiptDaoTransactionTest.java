package com.example.nzreceiptapp.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemRow;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.ItemDiscount;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @Test
    public void saveFullReceipt_itemFailureRollsBackStoreAndReceipt() {
        StoreEntity store = new StoreEntity(
                "new-store", "Woolworths", "Albany");
        ReceiptEntity receipt = new ReceiptEntity(
                "new-receipt", store.id,
                LocalDateTime.of(2026, 8, 17, 10, 30),
                0, false, "OCR", "image.jpg", 399L);
        ReceiptItemEntity invalidItem = new ReceiptItemEntity(
                "invalid-item", receipt.id, "Milk", "Milk",
                1, "ea", 399, "missing-category", false);

        try {
            receiptDao.saveFullReceipt(
                    store,
                    receipt,
                    Collections.singletonList(invalidItem),
                    Collections.emptyList());
            fail("Expected the foreign-key violation to roll back the insert");
        } catch (RuntimeException expected) {
            // The Store and Receipt writes must roll back with the invalid Item.
        }

        assertEquals(0, receiptDao.countReceipts());
        assertEquals(0, receiptDao.countReceiptItems());
        assertNull(receiptDao.findStore("woolworths", "albany"));
    }

    @Test
    public void updateFullReceipt_toExistingStore_reusesTargetAndRemovesOldStore() {
        saveReceipt("receipt-old", "store-old", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 10, 0), "old-item");
        saveReceipt("receipt-target", "store-target", "PAK'nSAVE", "Albany",
                LocalDateTime.of(2026, 8, 17, 11, 0), "target-item");
        ReceiptEntity edited = new ReceiptEntity(
                "receipt-old", "ignored-store-id",
                LocalDateTime.of(2026, 8, 17, 12, 0),
                0, false, "EDITED", "image.jpg", 500L);
        ReceiptItemEntity editedItem = new ReceiptItemEntity(
                "edited-item", edited.id, "Bread", "Bread",
                1, "ea", 500, null, false);

        receiptDao.updateFullReceipt(
                new StoreEntity("unused-new-id", " pak n save ", " ALBANY "),
                edited,
                Collections.singletonList(editedItem),
                Collections.emptyList());

        assertEquals("store-target",
                receiptDao.getReceiptEntityById("receipt-old").storeId);
        assertEquals("store-target",
                receiptDao.getReceiptEntityById("receipt-target").storeId);
        assertNull(receiptDao.findStore("woolworths", "albany"));
        assertNotNull(receiptDao.findStore("paknsave", "albany"));
    }

    @Test
    public void deleteReceiptAndUnusedStore_keepsSharedStoreUntilLastReceipt() {
        saveReceipt("receipt-first", "store-first", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 10, 0), "first-item");
        saveReceipt("receipt-second", "store-second", " wool-worths! ", " ALBANY ",
                LocalDateTime.of(2026, 8, 17, 11, 0), "second-item");

        receiptDao.deleteReceiptAndUnusedStore("receipt-first");

        assertNull(receiptDao.getReceiptById("receipt-first"));
        assertNotNull(receiptDao.getReceiptById("receipt-second"));
        assertNotNull(receiptDao.findStore("woolworths", "albany"));

        receiptDao.deleteReceiptAndUnusedStore("receipt-second");

        assertEquals(0, receiptDao.countReceipts());
        assertNull(receiptDao.findStore("woolworths", "albany"));
    }

    @Test
    public void getReceiptsPage_ordersByFullTimestampThenSavedFifo() {
        saveReceipt("old", "store-old", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 10, 0, 0), "old-item");
        saveReceipt("tie-first", "store-tie-first", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 12, 0, 0), "tie-first-item");
        saveReceipt("tie-second", "store-tie-second", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 12, 0, 0), "tie-second-item");
        saveReceipt("newest", "store-newest", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 12, 0, 1), "newest-item");

        ReceiptDao.PageData<ReceiptWithItems> firstPage =
                receiptDao.getReceiptsPage(1, 2);
        ReceiptDao.PageData<ReceiptWithItems> secondPage =
                receiptDao.getReceiptsPage(2, 2);
        ReceiptDao.PageData<ReceiptWithItems> clampedPage =
                receiptDao.getReceiptsPage(99, 2);

        assertEquals(4, firstPage.getTotalRecords());
        assertEquals("newest", firstPage.getRows().get(0).receipt.id);
        assertEquals("tie-first", firstPage.getRows().get(1).receipt.id);
        assertEquals("tie-second", secondPage.getRows().get(0).receipt.id);
        assertEquals("old", secondPage.getRows().get(1).receipt.id);
        assertEquals(2, clampedPage.getCurrentPage());
        assertEquals("tie-second", clampedPage.getRows().get(0).receipt.id);
    }

    @Test
    public void getAllItemsPage_ordersReceiptsThenKeepsItemInsertionOrder() {
        saveReceipt("older", "store-older", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 10, 0, 0), "older-item");
        saveReceipt("newer", "store-newer", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 11, 0, 0),
                "newer-first", "newer-second");

        ReceiptDao.PageData<ReceiptItemRow> firstPage =
                receiptDao.getAllItemsPage(1, 2);
        ReceiptDao.PageData<ReceiptItemRow> secondPage =
                receiptDao.getAllItemsPage(2, 2);

        assertEquals(3, firstPage.getTotalRecords());
        assertEquals("newer-first", firstPage.getRows().get(0).item.id);
        assertEquals("newer-second", firstPage.getRows().get(1).item.id);
        assertEquals("older-item", secondPage.getRows().get(0).item.id);
    }

    @Test
    public void getReceiptsInPurchaseHour_includesStartAndExcludesNextHour() {
        saveReceipt("at-start", "store-start", "Woolworths", "Albany",
                LocalDateTime.of(2026, 8, 17, 10, 0, 0), "start-item");
        saveReceipt("at-end-minus-second", "store-end-minus", "Woolworths", "CBD",
                LocalDateTime.of(2026, 8, 17, 10, 59, 59), "end-minus-item");
        saveReceipt("at-next-hour", "store-next", "Woolworths", "Greenlane",
                LocalDateTime.of(2026, 8, 17, 11, 0, 0), "next-item");
        saveReceipt("other-chain", "store-other", "PAK'nSAVE", "Albany",
                LocalDateTime.of(2026, 8, 17, 10, 30, 0), "other-item");

        List<ReceiptWithItems> matches = receiptDao.getReceiptsInPurchaseHour(
                "woolworths",
                LocalDateTime.of(2026, 8, 17, 10, 0, 0),
                LocalDateTime.of(2026, 8, 17, 11, 0, 0));

        assertEquals(2, matches.size());
        assertEquals("at-start", matches.get(0).receipt.id);
        assertEquals("at-end-minus-second", matches.get(1).receipt.id);
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

    private void saveReceipt(String receiptId,
                             String storeId,
                             String chain,
                             String branch,
                             LocalDateTime purchaseDate,
                             String... itemIds) {
        List<ReceiptItemEntity> items = new ArrayList<>();
        for (String itemId : itemIds) {
            items.add(new ReceiptItemEntity(
                    itemId, receiptId, itemId, itemId,
                    1, "ea", 100, null, false));
        }
        receiptDao.saveFullReceipt(
                new StoreEntity(storeId, chain, branch),
                new ReceiptEntity(
                        receiptId, storeId, purchaseDate,
                        0, false, "OCR", "image.jpg", 100L),
                items,
                Collections.emptyList());
    }
}
