package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.presentation.base.BaseViewModel;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 處理收據歷史清單與管理邏輯的 ViewModel
 */
public class HistoryViewModel extends BaseViewModel {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int[] PAGE_SIZE_OPTIONS = {15, 30, 50};

    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final DeleteReceiptUseCase deleteUseCase;
    private final Executor ioExecutor;

    private final MutableLiveData<List<Receipt>> receipts = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> pageSize = new MutableLiveData<>(DEFAULT_PAGE_SIZE);
    private final MutableLiveData<Integer> totalPages = new MutableLiveData<>(1);

    public HistoryViewModel(GetReceiptsPagedUseCase getReceiptsPagedUseCase,
                            DeleteReceiptUseCase deleteUseCase,
                            Executor ioExecutor) {
        this.getReceiptsPagedUseCase = getReceiptsPagedUseCase;
        this.deleteUseCase = deleteUseCase;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<Receipt>> getReceipts() { return receipts; }
    public LiveData<Integer> getCurrentPage() { return currentPage; }
    public LiveData<Integer> getPageSize() { return pageSize; }
    public LiveData<Integer> getTotalPages() { return totalPages; }

    public void setPageSize(int newPageSize) {
        // Accept only allowed sizes and avoid redundant reloads
        if (newPageSize != PAGE_SIZE_OPTIONS[0] && newPageSize != PAGE_SIZE_OPTIONS[1] && newPageSize != PAGE_SIZE_OPTIONS[2]) {
            return; // ignore invalid sizes
        }
        Integer current = pageSize.getValue();
        if (current != null && current == newPageSize) return; // no-op when unchanged

        pageSize.setValue(newPageSize);
        currentPage.setValue(0);
        loadData();
    }

    public void goToPage(int page) {
        Integer pages = totalPages.getValue();
        Integer cur = currentPage.getValue();
        if (page < 0 || pages == null || page >= pages) {
            return; // invalid page
        }
        if (cur != null && cur == page) return; // no-op when selecting current page

        currentPage.setValue(page);
        loadData();
    }

    public void nextPage() {
        int page = currentPage.getValue() != null ? currentPage.getValue() : 0;
        Integer pages = totalPages.getValue();
        if (pages == null) return;
        if (page + 1 >= pages) return; // already last page

        currentPage.setValue(page + 1);
        loadData();
    }

    public void prevPage() {
        int page = currentPage.getValue() != null ? currentPage.getValue() : 0;
        if (page <= 0) return; // already first page

        currentPage.setValue(page - 1);
        loadData();
    }

    public void loadData() {
        isLoading.setValue(true);
        int page = currentPage.getValue() != null ? currentPage.getValue() : 0;
        int size = pageSize.getValue() != null ? pageSize.getValue() : DEFAULT_PAGE_SIZE;

        ioExecutor.execute(() -> {
            try {
                PageResult<Receipt> result = getReceiptsPagedUseCase.executeWithCount(page, size);
                List<Receipt> list = result.items;
                int totalCount = result.totalCount;

                receipts.postValue(list);

                int pages = Math.max(1, (int) Math.ceil((double) totalCount / size));
                totalPages.postValue(pages);

                // Ensure current page is in range (could happen after page size change)
                int cur = currentPage.getValue() != null ? currentPage.getValue() : 0;
                if (cur >= pages) {
                    currentPage.postValue(pages - 1);
                }
            } catch (Exception e) {
                errorMessages.postValue("Failed to load history: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    /**
     * 刪除指定收據
     */
    public void deleteReceipt(String receiptId) {
        ioExecutor.execute(() -> {
            try {
                deleteUseCase.execute(receiptId);
                loadData();
            } catch (Exception e) {
                errorMessages.postValue("Delete failed: " + e.getMessage());
            }
        });
    }
}
