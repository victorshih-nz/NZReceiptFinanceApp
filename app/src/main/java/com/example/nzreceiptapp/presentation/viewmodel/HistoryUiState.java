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

    public enum LoadState {
        IDLE,
        INITIAL_LOADING,
        CONTENT,
        EMPTY,
        INITIAL_ERROR,
        REFRESHING,
        PAGE_CHANGING
    }

    private final ViewMode viewMode;
    private final List<Receipt> receipts;
    private final List<ReceiptItemSummary> allItems;
    private final PagingState receiptPaging;
    private final PagingState itemPaging;
    private final LoadState loadState;
    private final String errorMessage;
    private final String pendingDeleteReceiptId;
    private final String deletingReceiptId;

    private HistoryUiState(ViewMode viewMode,
                           List<Receipt> receipts,
                           List<ReceiptItemSummary> allItems,
                           PagingState receiptPaging,
                           PagingState itemPaging,
                           LoadState loadState,
                           String errorMessage,
                           String pendingDeleteReceiptId,
                           String deletingReceiptId) {
        this.viewMode = viewMode;
        this.receipts = immutableCopy(receipts);
        this.allItems = immutableCopy(allItems);
        this.receiptPaging = receiptPaging;
        this.itemPaging = itemPaging;
        this.loadState = loadState;
        this.errorMessage = errorMessage;
        this.pendingDeleteReceiptId = pendingDeleteReceiptId;
        this.deletingReceiptId = deletingReceiptId;
    }

    public static HistoryUiState initial(int receiptPageSize, int itemPageSize) {
        return new HistoryUiState(
                ViewMode.RECEIPTS,
                Collections.emptyList(),
                Collections.emptyList(),
                PagingState.initial(receiptPageSize),
                PagingState.initial(itemPageSize),
                LoadState.IDLE,
                null,
                null,
                null);
    }

    public HistoryUiState selectMode(ViewMode mode) {
        PagingState selectedPaging = getPaging(mode);
        return new HistoryUiState(
                mode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                settledState(mode, selectedPaging),
                null,
                pendingDeleteReceiptId,
                deletingReceiptId);
    }

    public HistoryUiState startLoading(LoadState loadingState) {
        if (loadingState != LoadState.INITIAL_LOADING
                && loadingState != LoadState.REFRESHING
                && loadingState != LoadState.PAGE_CHANGING) {
            throw new IllegalArgumentException("A loading state is required");
        }
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                loadingState,
                null,
                pendingDeleteReceiptId,
                deletingReceiptId);
    }

    public HistoryUiState withReceiptPage(PageResult<Receipt> result) {
        return new HistoryUiState(
                viewMode,
                result.getItems(),
                allItems,
                PagingState.from(result),
                itemPaging,
                result.getItems().isEmpty() ? LoadState.EMPTY : LoadState.CONTENT,
                null,
                pendingDeleteReceiptId,
                deletingReceiptId);
    }

    public HistoryUiState withItemPage(PageResult<ReceiptItemSummary> result) {
        return new HistoryUiState(
                viewMode,
                receipts,
                result.getItems(),
                receiptPaging,
                PagingState.from(result),
                result.getItems().isEmpty() ? LoadState.EMPTY : LoadState.CONTENT,
                null,
                pendingDeleteReceiptId,
                deletingReceiptId);
    }

    public HistoryUiState withInitialError(String message) {
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                LoadState.INITIAL_ERROR,
                message,
                pendingDeleteReceiptId,
                deletingReceiptId);
    }

    public HistoryUiState settle() {
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                settledState(viewMode, getActivePaging()),
                null,
                pendingDeleteReceiptId,
                deletingReceiptId);
    }

    public HistoryUiState requestDelete(String receiptId) {
        if (receiptId == null || receiptId.trim().isEmpty()) {
            throw new IllegalArgumentException("Receipt ID is required");
        }
        return copyDeleteState(receiptId, deletingReceiptId);
    }

    public HistoryUiState cancelDelete() {
        return copyDeleteState(null, deletingReceiptId);
    }

    private HistoryUiState copyDeleteState(String pendingId, String deletingId) {
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                loadState,
                errorMessage,
                pendingId,
                deletingId);
    }

    public HistoryUiState startDeleting(String receiptId) {
        if (receiptId == null || receiptId.trim().isEmpty()) {
            throw new IllegalArgumentException("Receipt ID is required");
        }
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                loadState,
                errorMessage,
                null,
                receiptId);
    }

    public HistoryUiState finishDeletingWithReceiptPage(PageResult<Receipt> result) {
        return new HistoryUiState(
                viewMode,
                result.getItems(),
                allItems,
                PagingState.from(result),
                itemPaging,
                result.getItems().isEmpty() ? LoadState.EMPTY : LoadState.CONTENT,
                null,
                null,
                null);
    }

    public HistoryUiState finishDeleting() {
        return new HistoryUiState(
                viewMode,
                receipts,
                allItems,
                receiptPaging,
                itemPaging,
                settledState(viewMode, getActivePaging()),
                null,
                null,
                null);
    }

    public HistoryUiState finishDeletingWithReloadError(String message) {
        return new HistoryUiState(
                viewMode,
                Collections.emptyList(),
                allItems,
                PagingState.initial(receiptPaging.getPageSize()),
                itemPaging,
                LoadState.INITIAL_ERROR,
                message,
                null,
                null);
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
        return getPaging(viewMode);
    }

    public PagingState getPaging(ViewMode mode) {
        return mode == ViewMode.ALL_ITEMS ? itemPaging : receiptPaging;
    }

    public LoadState getLoadState() {
        return loadState;
    }

    public boolean isLoading() {
        return loadState == LoadState.INITIAL_LOADING
                || loadState == LoadState.REFRESHING
                || loadState == LoadState.PAGE_CHANGING;
    }

    public boolean isRefreshing() {
        return loadState == LoadState.REFRESHING;
    }

    public boolean hasActiveSuccessfulPage() {
        return getActivePaging().hasLoaded();
    }

    public boolean shouldShowActiveContent() {
        return hasActiveSuccessfulPage() && !isActiveContentEmpty();
    }

    public boolean shouldShowActiveEmpty() {
        return hasActiveSuccessfulPage() && isActiveContentEmpty();
    }

    public boolean canUsePagingControls() {
        return hasActiveSuccessfulPage() && !isLoading() && !isDeletingReceipt();
    }

    public boolean isActiveContentEmpty() {
        return viewMode == ViewMode.ALL_ITEMS
                ? allItems.isEmpty()
                : receipts.isEmpty();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isDeletingReceipt() {
        return deletingReceiptId != null;
    }

    public String getPendingDeleteReceiptId() {
        return pendingDeleteReceiptId;
    }

    public String getDeletingReceiptId() {
        return deletingReceiptId;
    }

    private LoadState settledState(ViewMode mode, PagingState paging) {
        if (!paging.hasLoaded()) {
            return LoadState.IDLE;
        }
        boolean empty = mode == ViewMode.ALL_ITEMS
                ? allItems.isEmpty()
                : receipts.isEmpty();
        return empty ? LoadState.EMPTY : LoadState.CONTENT;
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
        private final boolean loaded;

        private PagingState(int currentPage,
                            int pageSize,
                            int totalRecords,
                            int totalPages,
                            boolean hasPrevious,
                            boolean hasNext,
                            boolean loaded) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalRecords = totalRecords;
            this.totalPages = totalPages;
            this.hasPrevious = hasPrevious;
            this.hasNext = hasNext;
            this.loaded = loaded;
        }

        private static PagingState initial(int pageSize) {
            return new PagingState(1, pageSize, 0, 1, false, false, false);
        }

        private static PagingState from(PageResult<?> result) {
            return new PagingState(
                    result.getCurrentPage(),
                    result.getPageSize(),
                    result.getTotalRecords(),
                    result.getTotalPages(),
                    result.hasPrevious(),
                    result.hasNext(),
                    true);
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

        public boolean hasLoaded() {
            return loaded;
        }
    }
}
