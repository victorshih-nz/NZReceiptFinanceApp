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
    private final MutableLiveData<HistoryEffect> effect =
            new MutableLiveData<>();
    private volatile HistoryUiState currentState = HistoryUiState.initial(
            DEFAULT_RECEIPT_PAGE_SIZE, DEFAULT_ITEM_PAGE_SIZE);
    private volatile PageRequest activeRequest;
    private volatile PageRequest failedInitialRequest;

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

    public LiveData<HistoryEffect> getEffect() {
        return effect;
    }

    public void setViewMode(HistoryUiState.ViewMode mode) {
        if (mode == null || mode == currentState.getViewMode()) {
            return;
        }
        publish(currentState.selectMode(mode));
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        loadPage(mode, paging.getCurrentPage(), paging.getPageSize());
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

    /** Loads History once for a newly created screen, without reloading retained state. */
    public void loadInitialData() {
        if (currentState.hasActiveSuccessfulPage()
                || currentState.getLoadState()
                == HistoryUiState.LoadState.INITIAL_ERROR) {
            return;
        }
        HistoryUiState.PagingState paging = currentState.getActivePaging();
        loadPage(currentState.getViewMode(),
                paging.getCurrentPage(), paging.getPageSize());
    }

    public void refresh() {
        loadData();
    }

    public void retry() {
        PageRequest request = failedInitialRequest;
        if (request == null
                || currentState.getLoadState()
                != HistoryUiState.LoadState.INITIAL_ERROR
                || request.mode != currentState.getViewMode()) {
            return;
        }
        loadPage(request.mode, request.page, request.pageSize);
    }

    private void loadPage(HistoryUiState.ViewMode mode, int page, int pageSize) {
        PageRequest existing = activeRequest;
        if (existing != null && existing.matches(mode, page, pageSize)) {
            return;
        }

        long requestId = requestSequence.incrementAndGet();
        PageRequest request = new PageRequest(requestId, mode, page, pageSize);
        HistoryUiState stateBeforeRequest = currentState;
        HistoryUiState.PagingState successfulPaging =
                stateBeforeRequest.getPaging(mode);
        HistoryUiState.LoadState loadingState;
        if (!successfulPaging.hasLoaded()) {
            loadingState = HistoryUiState.LoadState.INITIAL_LOADING;
        } else if (page == successfulPaging.getCurrentPage()
                && pageSize == successfulPaging.getPageSize()) {
            loadingState = HistoryUiState.LoadState.REFRESHING;
        } else {
            loadingState = HistoryUiState.LoadState.PAGE_CHANGING;
        }
        activeRequest = request;
        failedInitialRequest = null;
        publish(stateBeforeRequest.startLoading(loadingState));

        ioExecutor.execute(() -> {
            try {
                if (mode == HistoryUiState.ViewMode.RECEIPTS) {
                    PageResult<Receipt> result =
                            getReceiptsPagedUseCase.execute(page, pageSize);
                    if (!isCurrent(request)) return;
                    activeRequest = null;
                    publish(currentState.withReceiptPage(result));
                } else {
                    PageResult<ReceiptItemSummary> result =
                            getAllItemsPagedUseCase.execute(page, pageSize);
                    if (!isCurrent(request)) return;
                    activeRequest = null;
                    publish(currentState.withItemPage(result));
                }
            } catch (Exception exception) {
                if (isCurrent(request)) {
                    activeRequest = null;
                    if (successfulPaging.hasLoaded()) {
                        publish(stateBeforeRequest.settle());
                        publishEffect(loadingState
                                == HistoryUiState.LoadState.REFRESHING
                                ? HistoryEffect.refreshFailed()
                                : HistoryEffect.pageLoadFailed());
                    } else {
                        failedInitialRequest = request;
                        publish(currentState.withInitialError(
                                safeMessage(exception)));
                    }
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
                if (!currentState.hasActiveSuccessfulPage()) {
                    publish(currentState.withInitialError(
                            safeMessage(exception)));
                }
            }
        });
    }

    private boolean isCurrent(PageRequest request) {
        return request.id == requestSequence.get()
                && activeRequest == request;
    }

    private void publish(HistoryUiState state) {
        currentState = state;
        uiState.postValue(state);
    }

    private void publishEffect(HistoryEffect historyEffect) {
        effect.postValue(historyEffect);
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

    private static final class PageRequest {
        private final long id;
        private final HistoryUiState.ViewMode mode;
        private final int page;
        private final int pageSize;

        private PageRequest(long id,
                            HistoryUiState.ViewMode mode,
                            int page,
                            int pageSize) {
            this.id = id;
            this.mode = mode;
            this.page = page;
            this.pageSize = pageSize;
        }

        private boolean matches(HistoryUiState.ViewMode otherMode,
                                int otherPage,
                                int otherPageSize) {
            return mode == otherMode
                    && page == otherPage
                    && pageSize == otherPageSize;
        }
    }
}
