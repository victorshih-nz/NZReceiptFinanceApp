package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.presentation.base.BaseViewModel;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles receipt history content and one-based paging state.
 */
public class HistoryViewModel extends BaseViewModel {

    public enum ViewMode { RECEIPTS, ALL_ITEMS }

    private static final int DEFAULT_RECEIPT_PAGE_SIZE = 15;
    private static final int DEFAULT_ITEM_PAGE_SIZE = 30;

    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final GetAllItemsPagedUseCase getAllItemsPagedUseCase;
    private final DeleteReceiptUseCase deleteUseCase;
    private final Executor ioExecutor;
    private final AtomicLong requestSequence = new AtomicLong();

    private final MutableLiveData<List<Receipt>> receipts = new MutableLiveData<>();
    private final MutableLiveData<List<ReceiptItemSummary>> allItems = new MutableLiveData<>();
    private final MutableLiveData<ViewMode> viewMode =
            new MutableLiveData<>(ViewMode.RECEIPTS);

    private final ModePagingState receiptPaging =
            new ModePagingState(DEFAULT_RECEIPT_PAGE_SIZE);
    private final ModePagingState itemPaging =
            new ModePagingState(DEFAULT_ITEM_PAGE_SIZE);
    private final MutableLiveData<PagingUiState> pagingState =
            new MutableLiveData<>(receiptPaging.toUiState());

    public HistoryViewModel(GetReceiptsPagedUseCase getReceiptsPagedUseCase,
                            GetAllItemsPagedUseCase getAllItemsPagedUseCase,
                            DeleteReceiptUseCase deleteUseCase,
                            Executor ioExecutor) {
        this.getReceiptsPagedUseCase = getReceiptsPagedUseCase;
        this.getAllItemsPagedUseCase = getAllItemsPagedUseCase;
        this.deleteUseCase = deleteUseCase;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<Receipt>> getReceipts() {
        return receipts;
    }

    public LiveData<List<ReceiptItemSummary>> getAllItems() {
        return allItems;
    }

    public LiveData<ViewMode> getViewMode() {
        return viewMode;
    }

    public LiveData<PagingUiState> getPagingState() {
        return pagingState;
    }

    public void setViewMode(ViewMode mode) {
        if (mode == null) return;
        if (mode == viewMode.getValue()) {
            publishPagingState(mode);
            return;
        }

        viewMode.setValue(mode);
        publishPagingState(mode);
        loadData();
    }

    public void nextPage() {
        ModePagingState state = activePagingState();
        if (state.hasNext) {
            loadPage(activeMode(), state.currentPage + 1, state.pageSize);
        }
    }

    public void prevPage() {
        ModePagingState state = activePagingState();
        if (state.hasPrevious) {
            loadPage(activeMode(), state.currentPage - 1, state.pageSize);
        }
    }

    public void goToPage(int page) {
        ModePagingState state = activePagingState();
        if (page < 1 || page > state.totalPages || page == state.currentPage) {
            return;
        }
        loadPage(activeMode(), page, state.pageSize);
    }

    /** Reloads the active mode's last successful page. */
    public void loadData() {
        ViewMode mode = activeMode();
        ModePagingState state = pagingStateFor(mode);
        loadPage(mode, state.currentPage, state.pageSize);
    }

    private void loadPage(ViewMode mode, int page, int pageSize) {
        long requestId = requestSequence.incrementAndGet();
        isLoading.postValue(true);

        ioExecutor.execute(() -> {
            try {
                if (mode == ViewMode.RECEIPTS) {
                    PageResult<Receipt> result =
                            getReceiptsPagedUseCase.execute(page, pageSize);
                    if (requestId != requestSequence.get()) return;
                    receiptPaging.update(result);
                    receipts.postValue(result.getItems());
                } else {
                    PageResult<ReceiptItemSummary> result =
                            getAllItemsPagedUseCase.execute(page, pageSize);
                    if (requestId != requestSequence.get()) return;
                    itemPaging.update(result);
                    allItems.postValue(result.getItems());
                }

                if (mode == viewMode.getValue()) {
                    pagingState.postValue(pagingStateFor(mode).toUiState());
                }
            } catch (Exception exception) {
                if (requestId == requestSequence.get()) {
                    errorMessages.postValue(
                            "Failed to load history: " + safeMessage(exception));
                }
            } finally {
                if (requestId == requestSequence.get()) {
                    isLoading.postValue(false);
                }
            }
        });
    }

    /** Deletes the exact receipt and reloads the retained Receipt page. */
    public void deleteReceipt(String receiptId) {
        ioExecutor.execute(() -> {
            try {
                deleteUseCase.execute(receiptId);
                loadData();
            } catch (Exception exception) {
                errorMessages.postValue("Delete failed: " + safeMessage(exception));
            }
        });
    }

    private ViewMode activeMode() {
        ViewMode mode = viewMode.getValue();
        return mode == null ? ViewMode.RECEIPTS : mode;
    }

    private ModePagingState activePagingState() {
        return pagingStateFor(activeMode());
    }

    private ModePagingState pagingStateFor(ViewMode mode) {
        return mode == ViewMode.ALL_ITEMS ? itemPaging : receiptPaging;
    }

    private void publishPagingState(ViewMode mode) {
        pagingState.setValue(pagingStateFor(mode).toUiState());
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private static final class ModePagingState {
        private int currentPage = 1;
        private final int pageSize;
        private int totalPages = 1;
        private boolean hasPrevious;
        private boolean hasNext;

        private ModePagingState(int pageSize) {
            this.pageSize = pageSize;
        }

        private void update(PageResult<?> result) {
            currentPage = result.getCurrentPage();
            totalPages = result.getTotalPages();
            hasPrevious = result.hasPrevious();
            hasNext = result.hasNext();
        }

        private PagingUiState toUiState() {
            return new PagingUiState(
                    currentPage, pageSize, totalPages, hasPrevious, hasNext);
        }
    }

    public static final class PagingUiState {
        private final int currentPage;
        private final int pageSize;
        private final int totalPages;
        private final boolean hasPrevious;
        private final boolean hasNext;

        private PagingUiState(int currentPage,
                              int pageSize,
                              int totalPages,
                              boolean hasPrevious,
                              boolean hasNext) {
            this.currentPage = currentPage;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
            this.hasPrevious = hasPrevious;
            this.hasNext = hasNext;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getPageSize() {
            return pageSize;
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
