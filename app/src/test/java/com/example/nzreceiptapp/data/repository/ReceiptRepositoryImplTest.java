package com.example.nzreceiptapp.data.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemRow;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.service.IReceiptImageStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ReceiptRepositoryImplTest {
    @Mock private ReceiptDao receiptDao;
    @Mock private IReceiptImageStore imageStore;

    private ReceiptRepositoryImpl repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new ReceiptRepositoryImpl(receiptDao, imageStore);
    }

    @Test
    public void saveReceipt_preservesSourceMetadata() {
        ReceiptItem item = new ReceiptItem(
                "item", "raw milk", "Milk", 1, "ea", 399,
                Collections.emptyList(), null, false);
        Receipt receipt = new Receipt(
                "receipt", new Store("store", "Woolworths", "Albany"),
                Collections.singletonList(item), LocalDateTime.of(2026, 8, 7, 10, 0),
                0, false, "OCR TEXT", "file:///receipt.jpg", 399L);

        repository.saveReceipt(receipt);

        ArgumentCaptor<ReceiptEntity> captor = ArgumentCaptor.forClass(ReceiptEntity.class);
        ArgumentCaptor<StoreEntity> storeCaptor =
                ArgumentCaptor.forClass(StoreEntity.class);
        verify(receiptDao).saveFullReceipt(
                storeCaptor.capture(), captor.capture(), any(), any());
        assertEquals("woolworths", storeCaptor.getValue().normalizedChain);
        assertEquals("albany", storeCaptor.getValue().normalizedBranch);
        assertEquals("OCR TEXT", captor.getValue().rawOcrText);
        assertEquals("file:///receipt.jpg", captor.getValue().imageUri);
        assertEquals(Long.valueOf(399), captor.getValue().printedTotalCents);
    }

    @Test
    public void deleteReceipt_removesPersistedImageAfterDatabaseDelete() {
        ReceiptWithItems stored = new ReceiptWithItems();
        stored.receipt = new ReceiptEntity(
                "receipt", "store", LocalDateTime.now(), 0, false,
                "OCR", "file:///receipt.jpg", 399L);
        when(receiptDao.getReceiptById("receipt")).thenReturn(stored);

        repository.deleteReceipt("receipt");

        verify(receiptDao).deleteReceiptAndUnusedStore("receipt");
        verify(imageStore).delete("file:///receipt.jpg");
    }

    @Test
    public void getReceiptsPage_mapsRowsAndMetadata() {
        ReceiptWithItems stored = new ReceiptWithItems();
        stored.receipt = new ReceiptEntity(
                "receipt", "store", LocalDateTime.of(2026, 8, 17, 10, 0),
                0, false);
        stored.store = new StoreEntity("store", "Woolworths", "Albany");
        stored.items = Collections.emptyList();
        ReceiptDao.PageData<ReceiptWithItems> pageData = new ReceiptDao.PageData<>(
                Collections.singletonList(stored), 2, 31);
        when(receiptDao.getReceiptsPage(2, 15)).thenReturn(pageData);

        PageResult<Receipt> result = repository.getReceiptsPage(2, 15);

        assertEquals(1, result.getItems().size());
        assertEquals("receipt", result.getItems().get(0).getId());
        assertEquals(2, result.getCurrentPage());
        assertEquals(15, result.getPageSize());
        assertEquals(31, result.getTotalRecords());
        assertEquals(3, result.getTotalPages());
        verify(receiptDao).getReceiptsPage(2, 15);
    }

    @Test
    public void getAllItemsPage_mapsRowsAndMetadata() {
        ReceiptItemRow row = new ReceiptItemRow();
        row.item = new ReceiptItemEntity(
                "item", "receipt", "Milk", "Milk", 1,
                "ea", 399, null, false);
        row.chainName = "Woolworths";
        row.branchName = "Albany";
        row.purchaseDate = LocalDateTime.of(2026, 8, 17, 10, 0);
        row.discounts = Collections.emptyList();
        ReceiptDao.PageData<ReceiptItemRow> pageData = new ReceiptDao.PageData<>(
                Collections.singletonList(row), 2, 61);
        when(receiptDao.getAllItemsPage(2, 30)).thenReturn(pageData);

        PageResult<ReceiptItemSummary> result = repository.getAllItemsPage(2, 30);

        assertEquals(1, result.getItems().size());
        assertEquals("item", result.getItems().get(0).getItem().getId());
        assertEquals(2, result.getCurrentPage());
        assertEquals(30, result.getPageSize());
        assertEquals(61, result.getTotalRecords());
        assertEquals(3, result.getTotalPages());
        verify(receiptDao).getAllItemsPage(2, 30);
    }

    @Test
    public void findDuplicateCandidates_filtersNormalizedChainWithinHour() {
        LocalDateTime hourStart = LocalDateTime.of(2026, 8, 17, 10, 0);
        LocalDateTime hourEnd = hourStart.plusHours(1);
        ReceiptWithItems matching = storedReceipt(
                "matching", " wool-worths! ", "Different Branch", hourStart.plusMinutes(5));
        when(receiptDao.getReceiptsInPurchaseHour(
                "woolworths", hourStart, hourEnd))
                .thenReturn(Collections.singletonList(matching));

        List<Receipt> result = repository.findDuplicateCandidates(
                "woolworths", hourStart, hourEnd);

        assertEquals(1, result.size());
        assertEquals("matching", result.get(0).getId());
        assertEquals(" wool-worths! ", result.get(0).getStore().getChainName());
        verify(receiptDao).getReceiptsInPurchaseHour(
                "woolworths", hourStart, hourEnd);
    }

    private ReceiptWithItems storedReceipt(String receiptId,
                                           String chain,
                                           String branch,
                                           LocalDateTime purchaseDate) {
        ReceiptWithItems stored = new ReceiptWithItems();
        stored.receipt = new ReceiptEntity(
                receiptId, "store-" + receiptId, purchaseDate, 0, false);
        stored.store = new StoreEntity(
                "store-" + receiptId, chain, branch);
        stored.items = Collections.emptyList();
        return stored;
    }
}
