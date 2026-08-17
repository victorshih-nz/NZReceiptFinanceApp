package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryUiStateTest {

    @Test
    public void withReceiptPage_copiesContentAndKeepsIndependentItemPaging() {
        HistoryUiState initial = HistoryUiState.initial(15, 30);
        List<Receipt> source = new ArrayList<>();
        source.add(receipt("receipt-1"));

        HistoryUiState updated = initial.withReceiptPage(
                new PageResult<>(source, 2, 15, 31));
        source.clear();

        assertEquals(1, updated.getReceipts().size());
        assertEquals("receipt-1", updated.getReceipts().get(0).getId());
        assertEquals(2, updated.getReceiptPaging().getCurrentPage());
        assertEquals(3, updated.getReceiptPaging().getTotalPages());
        assertEquals(1, updated.getItemPaging().getCurrentPage());
        assertEquals(30, updated.getItemPaging().getPageSize());
        assertEquals(HistoryUiState.LoadState.CONTENT, updated.getLoadState());
        assertThrows(UnsupportedOperationException.class,
                () -> updated.getReceipts().add(receipt("receipt-2")));
    }

    @Test
    public void emptyResult_usesPageOneOfOneAndEmptyState() {
        HistoryUiState updated = HistoryUiState.initial(15, 30)
                .withReceiptPage(new PageResult<>(
                        Collections.emptyList(), 1, 15, 0));

        assertEquals(HistoryUiState.LoadState.EMPTY, updated.getLoadState());
        assertEquals(1, updated.getActivePaging().getCurrentPage());
        assertEquals(1, updated.getActivePaging().getTotalPages());
    }

    private Receipt receipt(String id) {
        return new Receipt(id, null, null, null, 0, false);
    }
}
