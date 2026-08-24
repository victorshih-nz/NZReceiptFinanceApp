package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.presentation.base.BaseViewModel;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 處理收據歷史清單與管理邏輯的 ViewModel
 */
public class HistoryViewModel extends BaseViewModel {

    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final DeleteReceiptUseCase deleteUseCase;
    private final Executor ioExecutor;

    private final MutableLiveData<List<Receipt>> receipts = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);

    public HistoryViewModel(GetReceiptsPagedUseCase getReceiptsPagedUseCase,
                            DeleteReceiptUseCase deleteUseCase,
                            Executor ioExecutor) {
        this.getReceiptsPagedUseCase = getReceiptsPagedUseCase;
        this.deleteUseCase = deleteUseCase;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<List<Receipt>> getReceipts() { return receipts; }
    public LiveData<Integer> getCurrentPage() { return currentPage; }

    public void nextPage() {
        int page = currentPage.getValue() != null ? currentPage.getValue() : 0;
        currentPage.setValue(page + 1);
        loadData();
    }

    public void prevPage() {
        int page = currentPage.getValue() != null ? currentPage.getValue() : 0;
        if (page > 0) {
            currentPage.setValue(page - 1);
            loadData();
        }
    }

    public void loadData() {
        isLoading.setValue(true);
        int page = currentPage.getValue() != null ? currentPage.getValue() : 0;

        ioExecutor.execute(() -> {
            try {
                List<Receipt> list = getReceiptsPagedUseCase.execute(page, 10);
                receipts.postValue(list);
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
