package com.example.nzreceiptapp.data.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.service.IReceiptImageStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

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
        verify(receiptDao).saveFullReceipt(any(), captor.capture(), any(), any());
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
    public void getReceiptsCount_delegatesToDao() {
        when(receiptDao.countReceipts()).thenReturn(42);
        int count = repository.getReceiptsCount();
        assertEquals(42, count);
    }
}
