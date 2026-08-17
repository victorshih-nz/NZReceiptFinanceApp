package com.example.nzreceiptapp.data.local.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;

import org.junit.Before;
import org.junit.Test;

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
}
