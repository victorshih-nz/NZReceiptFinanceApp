package com.example.nzreceiptapp.presentation.viewmodel;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One immutable snapshot of everything needed to render History.
 */
public final class HistoryUiState {

    public enum ViewMode { RECEIPTS, ALL_ITEMS }

    public enum LoadState { IDLE, LOADING, CONTENT, EMPTY, ERROR }

    private final ViewMode viewMode;
    private final List<Receipt> receipts;
    private final List<ReceiptItemSummary> allItems;
    private final PagingState receiptPaging;
    private final PagingState itemPaging;
    private final LoadState loadState;
    private final String errorMessage;

    private HistoryUiState(ViewMode viewMode,
                           List<Receipt> receipts,
                           List<ReceiptItemSummary> allItems,
                           PagingState receiptPaging,
                           PagingState itemPaging,
                           LoadState loadState,
                           String errorMessage) {
        this.viewMode = viewMode;
        this.receipts = immutableCopy(receipts);
        this.allItems = immutableCopy(allItems);
        this.receiptPaging = receiptPaging;
        this.itemPaging = itemPaging;
        this.loadState = loadState;
        this.errorMessage = errorMessage;
    }

    public static HistoryUiState initial(int receiptPageSize, int itemPageSize) {
        return new HistoryUiState(
                ViewMode.RECEIPTS,
                Collections.emptyList(),
                Collections.emptyList(),
                PagingState.initial(receiptPageSize),
                PagingState.initial(itemPageSize),
                LoadState.IDLE,
                null);
    }

    public HistoryUiState selectMode(ViewMode mode) {
        return new HistoryUiState(
                mode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                loadState,
                null);
    }

    public HistoryUiState startLoading(int requestedPage, int requestedPageSize) {
        PagingState requestedPaging = getActivePaging()
                .forRequest(requestedPage, requestedPageSize);
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                viewMode == ViewMode.RECEIPTS ? requestedPaging : receiptPaging,
                viewMode == ViewMode.ALL_ITEMS ? requestedPaging : itemPaging,
                LoadState.LOADING,
                null);
    }

    public HistoryUiState withReceiptPage(PageResult<Receipt> result) {
        return new HistoryUiState(
                viewMode,
                result.getItems(),
                allItems,
                PagingState.from(result),
                itemPaging,
                result.getItems().isEmpty() ? LoadState.EMPTY : LoadState.CONTENT,
                null);
    }

    public HistoryUiState withItemPage(PageResult<ReceiptItemSummary> result) {
        return new HistoryUiState(
                viewMode,
                receipts,
                result.getItems(),
                receiptPaging,
                PagingState.from(result),
                result.getItems().isEmpty() ? LoadState.EMPTY : LoadState.CONTENT,
                null);
    }

    public HistoryUiState withError(String message) {
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                LoadState.ERROR,
                message);
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public List<Receipt> getReceipts() {
        return receipts;
    }

    public List<ReceiptItemSummary> getAllItems() {
        return allItems;
    }

    public PagingState getReceiptPaging() {
        return receiptPaging;
    }

    public PagingState getItemPaging() {
        return itemPaging;
    }

    public PagingState getActivePaging() {
        return viewMode == ViewMode.ALL_ITEMS ? itemPaging : receiptPaging;
    }

    public LoadState getLoadState() {
        return loadState;
    }

    public boolean isLoading() {
        return loadState == LoadState.LOADING;
    }

    public boolean isActiveContentEmpty() {
        return viewMode == ViewMode.ALL_ITEMS
                ? allItems.isEmpty()
                : receipts.isEmpty();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    private static <T> List<T> immutableCopy(List<T> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    public static final class PagingState {
        private final int currentPage;
        private final int pageSize;
        private final int totalRecords;
        private final int totalPages;
        private final boolean hasPrevious;
        private final boolean hasNext;

        private PagingState(int currentPage,
                            int pageSize,
                            int totalRecords,
                            int totalPages,
                            boolean hasPrevious,
                            boolean hasNext) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalRecords = totalRecords;
            this.totalPages = totalPages;
            this.hasPrevious = hasPrevious;
            this.hasNext = hasNext;
        }

        private static PagingState initial(int pageSize) {
            return new PagingState(1, pageSize, 0, 1, false, false);
        }

        private static PagingState from(PageResult<?> result) {
            return new PagingState(
                    result.getCurrentPage(),
                    result.getPageSize(),
                    result.getTotalRecords(),
                    result.getTotalPages(),
                    result.hasPrevious(),
                    result.hasNext());
        }

        private PagingState forRequest(int requestedPage, int requestedPageSize) {
            int requestedTotalPages = totalRecords == 0
                    ? 1
                    : ((totalRecords - 1) / requestedPageSize) + 1;
            int effectivePage = Math.max(
                    1, Math.min(requestedPage, requestedTotalPages));
            return new PagingState(
                    effectivePage,
                    requestedPageSize,
                    totalRecords,
                    requestedTotalPages,
                    effectivePage > 1,
                    totalRecords > 0 && effectivePage < requestedTotalPages);
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public boolean hasPrevious() {
            return hasPrevious;
        }

        public boolean hasNext() {
            return hasNext;
        }
    }
}
