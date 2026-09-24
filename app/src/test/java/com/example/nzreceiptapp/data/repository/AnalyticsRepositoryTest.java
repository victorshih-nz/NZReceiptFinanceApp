package com.example.nzreceiptapp.data.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.Receipt;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class AnalyticsRepositoryTest {
    @Test public void boundedQueryUsesExactDatesAndMapsResult() {
        ReceiptDao dao = mock(ReceiptDao.class);
        ReceiptRepositoryImpl repository = new ReceiptRepositoryImpl(dao);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 0, 0);
        ReceiptWithItems row = new ReceiptWithItems();
        row.receipt = new ReceiptEntity("r", "s", start, 0, false);
        row.store = new StoreEntity("s", "Woolworths", "Auckland");
        row.items = Collections.emptyList();
        when(dao.getReceiptsBetween(start, end)).thenReturn(Collections.singletonList(row));

        List<Receipt> result = repository.getReceiptsBetween(start, end);
        assertEquals(1, result.size());
        assertEquals("r", result.get(0).getId());
        verify(dao).getReceiptsBetween(start, end);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidRangeCannotReachDao() {
        ReceiptRepositoryImpl repository = new ReceiptRepositoryImpl(mock(ReceiptDao.class));
        LocalDateTime date = LocalDateTime.of(2026, 1, 1, 0, 0);
        repository.getReceiptsBetween(date, date);
    }
}
