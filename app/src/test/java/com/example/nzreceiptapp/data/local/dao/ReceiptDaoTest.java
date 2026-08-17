package com.example.nzreceiptapp.data.local.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ReceiptDaoTest {
    private ReceiptDao receiptDao;

    @Before
    public void setUp() {
        receiptDao = mock(ReceiptDao.class, CALLS_REAL_METHODS);
    }

    @Test
    public void getReceiptsPage_readsCountAndRequestedPageInOneMethod() {
        List<ReceiptWithItems> rows =
                Collections.singletonList(new ReceiptWithItems());
        when(receiptDao.countReceipts()).thenReturn(31);
        when(receiptDao.getReceiptsPaged(15, 15)).thenReturn(rows);

        ReceiptDao.PageData<ReceiptWithItems> result =
                receiptDao.getReceiptsPage(2, 15);

        assertSame(rows, result.getRows());
        assertEquals(2, result.getCurrentPage());
        assertEquals(31, result.getTotalRecords());
        verify(receiptDao).countReceipts();
        verify(receiptDao).getReceiptsPaged(15, 15);
    }

    @Test
    public void getReceiptsPage_missingRequestedPageUsesLastValidPage() {
        List<ReceiptWithItems> rows =
                Collections.singletonList(new ReceiptWithItems());
        when(receiptDao.countReceipts()).thenReturn(16);
        when(receiptDao.getReceiptsPaged(15, 15)).thenReturn(rows);

        ReceiptDao.PageData<ReceiptWithItems> result =
                receiptDao.getReceiptsPage(3, 15);

        assertSame(rows, result.getRows());
        assertEquals(2, result.getCurrentPage());
        assertEquals(16, result.getTotalRecords());
        verify(receiptDao).getReceiptsPaged(15, 15);
    }

    @Test
    public void getReceiptsPage_zeroRecordsUsesPageOneAndZeroOffset() {
        when(receiptDao.countReceipts()).thenReturn(0);
        when(receiptDao.getReceiptsPaged(15, 0))
                .thenReturn(Collections.emptyList());

        ReceiptDao.PageData<ReceiptWithItems> result =
                receiptDao.getReceiptsPage(1, 15);

        assertEquals(1, result.getCurrentPage());
        assertEquals(0, result.getTotalRecords());
        assertEquals(Collections.emptyList(), result.getRows());
        verify(receiptDao).getReceiptsPaged(15, 0);
    }

    @Test
    public void getReceiptsPage_rejectsPageBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> receiptDao.getReceiptsPage(0, 15));
    }

    @Test
    public void getReceiptsPage_rejectsNonPositivePageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> receiptDao.getReceiptsPage(1, 0));
    }

    @Test
    public void getAllItemsPage_readsItemCountAndRequestedPage() {
        when(receiptDao.countReceiptItems()).thenReturn(61);
        when(receiptDao.getAllItemsPaged(30, 30))
                .thenReturn(Collections.emptyList());

        ReceiptDao.PageData<?> result = receiptDao.getAllItemsPage(2, 30);

        assertEquals(2, result.getCurrentPage());
        assertEquals(61, result.getTotalRecords());
        verify(receiptDao).countReceiptItems();
        verify(receiptDao).getAllItemsPaged(30, 30);
    }

    @Test
    public void getAllItemsPage_missingRequestedPageUsesLastValidPage() {
        when(receiptDao.countReceiptItems()).thenReturn(31);
        when(receiptDao.getAllItemsPaged(30, 30))
                .thenReturn(Collections.emptyList());

        ReceiptDao.PageData<?> result = receiptDao.getAllItemsPage(3, 30);

        assertEquals(2, result.getCurrentPage());
        assertEquals(31, result.getTotalRecords());
        verify(receiptDao).getAllItemsPaged(30, 30);
    }

    @Test
    public void saveFullReceipt_reusesNormalizedStoreAndAllocatesSequence() {
        StoreEntity incoming = new StoreEntity(
                "new-store", " Wool-worths! ", " Green Lane! ");
        StoreEntity existing = new StoreEntity(
                "existing-store", "Woolworths", "Greenlane");
        ReceiptEntity receipt = receiptEntity("receipt-1", incoming.id);
        when(receiptDao.findStore("woolworths", "greenlane"))
                .thenReturn(existing);
        when(receiptDao.getNextSavedSequence()).thenReturn(8L);

        receiptDao.saveFullReceipt(incoming, receipt,
                Collections.<ReceiptItemEntity>emptyList(),
                Collections.emptyList());

        verify(receiptDao, never()).insertStore(incoming);
        verify(receiptDao).insertReceipt(receipt);
        assertEquals("existing-store", receipt.storeId);
        assertEquals(8L, receipt.savedSequence);
    }

    @Test
    public void saveFullReceipt_createsMissingNormalizedStore() {
        StoreEntity store = new StoreEntity(
                "store-1", "PAK'nSAVE", null);
        ReceiptEntity receipt = receiptEntity("receipt-1", store.id);
        when(receiptDao.findStore("paknsave", ""))
                .thenReturn(null, store);
        when(receiptDao.getNextSavedSequence()).thenReturn(1L);

        receiptDao.saveFullReceipt(store, receipt,
                Collections.<ReceiptItemEntity>emptyList(),
                Collections.emptyList());

        verify(receiptDao).insertStore(store);
        verify(receiptDao).insertReceipt(receipt);
        assertEquals("store-1", receipt.storeId);
        assertEquals(1L, receipt.savedSequence);
    }

    @Test
    public void updateFullReceipt_preservesSequenceAndReplacesOwnedItems() {
        ReceiptEntity existing = receiptEntity("receipt-1", "old-store");
        existing.savedSequence = 7L;
        ReceiptEntity edited = receiptEntity("receipt-1", "draft-store");
        StoreEntity targetStore = new StoreEntity(
                "target-store", "PAK'nSAVE", "Albany");
        List<ReceiptItemEntity> items = Collections.singletonList(
                new ReceiptItemEntity("new-item", "receipt-1", "Milk",
                        "Milk", 1, "ea", 399, null, false));
        when(receiptDao.getReceiptEntityById("receipt-1"))
                .thenReturn(existing);
        when(receiptDao.findStore("paknsave", "albany"))
                .thenReturn(targetStore);
        when(receiptDao.updateReceiptRow(edited)).thenReturn(1);

        receiptDao.updateFullReceipt(targetStore, edited, items,
                Collections.emptyList());

        assertEquals("target-store", edited.storeId);
        assertEquals(7L, edited.savedSequence);
        InOrder order = inOrder(receiptDao);
        order.verify(receiptDao).updateReceiptRow(edited);
        order.verify(receiptDao).deleteItemsByReceiptId("receipt-1");
        order.verify(receiptDao).insertReceiptItems(items);
        order.verify(receiptDao).deleteStoreIfUnused("old-store");
    }

    @Test
    public void updateFullReceipt_missingReceiptStopsBeforeWriting() {
        StoreEntity store = new StoreEntity(
                "store-1", "Woolworths", "Albany");
        ReceiptEntity receipt = receiptEntity("missing", store.id);
        when(receiptDao.getReceiptEntityById("missing")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> receiptDao.updateFullReceipt(store, receipt,
                        Collections.emptyList(), Collections.emptyList()));

        verify(receiptDao, never()).updateReceiptRow(receipt);
        verify(receiptDao, never()).deleteItemsByReceiptId("missing");
    }

    private ReceiptEntity receiptEntity(String id, String storeId) {
        return new ReceiptEntity(id, storeId,
                LocalDateTime.of(2026, 8, 17, 10, 0),
                0, false);
    }
}
