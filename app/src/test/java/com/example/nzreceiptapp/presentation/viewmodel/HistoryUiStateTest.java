package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void pageChanging_keepsLastSuccessfulPagingAndContent() {
        HistoryUiState receiptState = HistoryUiState.initial(15, 30)
                .withReceiptPage(new PageResult<>(
                        Collections.singletonList(receipt("receipt-1")),
                        2, 15, 31));

        HistoryUiState loading = receiptState.startLoading(
                HistoryUiState.LoadState.PAGE_CHANGING);

        assertEquals(HistoryUiState.LoadState.PAGE_CHANGING,
                loading.getLoadState());
        assertEquals("receipt-1", loading.getReceipts().get(0).getId());
        assertEquals(2, loading.getReceiptPaging().getCurrentPage());
        assertEquals(15, loading.getReceiptPaging().getPageSize());
        assertEquals(1, loading.getItemPaging().getCurrentPage());
        assertEquals(30, loading.getItemPaging().getPageSize());
        assertTrue(loading.isLoading());
        assertFalse(loading.isRefreshing());
    }

    @Test
    public void selectMode_derivesStateFromThatModesSuccessfulPage() {
        HistoryUiState receiptContent = HistoryUiState.initial(15, 30)
                .withReceiptPage(new PageResult<>(
                        Collections.singletonList(receipt("receipt-1")),
                        1, 15, 1));

        HistoryUiState unloadedItems = receiptContent.selectMode(
                HistoryUiState.ViewMode.ALL_ITEMS);
        HistoryUiState emptyItems = unloadedItems.withItemPage(
                new PageResult<>(Collections.emptyList(), 1, 30, 0));

        assertEquals(HistoryUiState.LoadState.IDLE,
                unloadedItems.getLoadState());
        assertFalse(unloadedItems.hasActiveSuccessfulPage());
        assertEquals(HistoryUiState.LoadState.EMPTY,
                emptyItems.getLoadState());
        assertTrue(emptyItems.hasActiveSuccessfulPage());
        assertEquals(HistoryUiState.LoadState.CONTENT,
                emptyItems.selectMode(HistoryUiState.ViewMode.RECEIPTS)
                        .getLoadState());
    }

    @Test
    public void settle_restoresContentAfterRecoverableLoadingState() {
        HistoryUiState content = HistoryUiState.initial(15, 30)
                .withReceiptPage(new PageResult<>(
                        Collections.singletonList(receipt("receipt-1")),
                        1, 15, 1));

        HistoryUiState settled = content
                .startLoading(HistoryUiState.LoadState.REFRESHING)
                .settle();

        assertEquals(HistoryUiState.LoadState.CONTENT,
                settled.getLoadState());
        assertEquals("receipt-1", settled.getReceipts().get(0).getId());
    }

    @Test
    public void refreshingEmpty_keepsEmptyVisibleAndDisablesPaging() {
        HistoryUiState refreshing = HistoryUiState.initial(15, 30)
                .withReceiptPage(new PageResult<>(
                        Collections.emptyList(), 1, 15, 0))
                .startLoading(HistoryUiState.LoadState.REFRESHING);

        assertTrue(refreshing.shouldShowActiveEmpty());
        assertFalse(refreshing.shouldShowActiveContent());
        assertFalse(refreshing.canUsePagingControls());
    }

    @Test
    public void initialError_hasNoContentAndDisablesPaging() {
        HistoryUiState error = HistoryUiState.initial(15, 30)
                .withInitialError("Unavailable");

        assertFalse(error.shouldShowActiveEmpty());
        assertFalse(error.shouldShowActiveContent());
        assertFalse(error.canUsePagingControls());
    }

    private Receipt receipt(String id) {
        return new Receipt(id, null, null, null, 0, false);
    }
}
