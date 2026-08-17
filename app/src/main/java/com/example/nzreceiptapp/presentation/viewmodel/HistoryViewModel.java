package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles receipt history content through one immutable UI state stream.
 */
public class HistoryViewModel extends ViewModel {

    private static final int DEFAULT_RECEIPT_PAGE_SIZE = 15;
    private static final int DEFAULT_ITEM_PAGE_SIZE = 30;
    private static final int[] SUPPORTED_PAGE_SIZES = {15, 30, 50};

    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final GetAllItemsPagedUseCase getAllItemsPagedUseCase;
    private final DeleteReceiptUseCase deleteUseCase;
    private final Executor ioExecutor;
    private final AtomicLong requestSequence = new AtomicLong();

    private final MutableLiveData<HistoryUiState> uiState =
            new MutableLiveData<>();
    private volatile HistoryUiState currentState = HistoryUiState.initial(
            DEFAULT_RECEIPT_PAGE_SIZE, DEFAULT_ITEM_PAGE_SIZE);

    public HistoryViewModel(GetReceiptsPagedUseCase getReceiptsPagedUseCase,
                            GetAllItemsPagedUseCase getAllItemsPagedUseCase,
                            DeleteReceiptUseCase deleteUseCase,
                            Executor ioExecutor) {
        this.getReceiptsPagedUseCase = getReceiptsPagedUseCase;
        this.getAllItemsPagedUseCase = getAllItemsPagedUseCase;
        this.deleteUseCase = deleteUseCase;
        this.ioExecutor = ioExecutor;
        uiState.setValue(currentState);
    }

    public LiveData<HistoryUiState> getUiState() {
        return uiState;
    }

    public void setViewMode(HistoryUiState.ViewMode mode) {
        if (mode == null || mode == currentState.getViewMode()) {
            return;
        }
        publish(currentState.selectMode(mode));
        loadData();
    }

    public void nextPage() {
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        if (paging.hasNext()) {
            loadPage(currentState.getViewMode(),
                    paging.getCurrentPage() + 1,
                    paging.getPageSize());
        }
    }

    public void prevPage() {
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        if (paging.hasPrevious()) {
            loadPage(currentState.getViewMode(),
                    paging.getCurrentPage() - 1,
                    paging.getPageSize());
        }
    }

    public void goToPage(int page) {
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        if (page < 1
                || page > paging.getTotalPages()
                || page == paging.getCurrentPage()) {
            return;
        }
        loadPage(currentState.getViewMode(), page, paging.getPageSize());
    }

    public void setPageSize(int pageSize) {
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        if (!isSupportedPageSize(pageSize) || pageSize == paging.getPageSize()) {
            return;
        }
        loadPage(currentState.getViewMode(), 1, pageSize);
    }

    /** Reloads the active mode's last successful page. */
    public void loadData() {
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        loadPage(currentState.getViewMode(),
                paging.getCurrentPage(), paging.getPageSize());
    }

    private void loadPage(HistoryUiState.ViewMode mode, int page, int pageSize) {
        long requestId = requestSequence.incrementAndGet();
        publish(currentState.startLoading(page, pageSize));

        ioExecutor.execute(() -> {
            try {
                if (mode == HistoryUiState.ViewMode.RECEIPTS) {
                    PageResult<Receipt> result =
                            getReceiptsPagedUseCase.execute(page, pageSize);
                    if (requestId != requestSequence.get()) return;
                    publish(currentState.withReceiptPage(result));
                } else {
                    PageResult<ReceiptItemSummary> result =
                            getAllItemsPagedUseCase.execute(page, pageSize);
                    if (requestId != requestSequence.get()) return;
                    publish(currentState.withItemPage(result));
                }
            } catch (Exception exception) {
                if (requestId == requestSequence.get()) {
                    publish(currentState.withError(
                            "Failed to load history: " + safeMessage(exception)));
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
                publish(currentState.withError(
                        "Delete failed: " + safeMessage(exception)));
            }
        });
    }

    private void publish(HistoryUiState state) {
        currentState = state;
        uiState.postValue(state);
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private boolean isSupportedPageSize(int pageSize) {
        for (int supported : SUPPORTED_PAGE_SIZES) {
            if (pageSize == supported) return true;
        }
        return false;
    }
}
