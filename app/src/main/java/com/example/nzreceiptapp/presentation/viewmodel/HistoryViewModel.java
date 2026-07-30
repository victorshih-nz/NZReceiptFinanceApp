package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsUseCase;
import com.example.nzreceiptapp.presentation.base.BaseViewModel;

import java.util.List;

import android.util.Log;

/**
 * 處理收據歷史清單與管理邏輯的 ViewModel
 */
public class HistoryViewModel extends BaseViewModel {

    private static final String TAG = "HistoryViewModel";

    public enum ViewMode { RECEIPTS, ALL_ITEMS }

    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final GetAllItemsPagedUseCase getAllItemsPagedUseCase;
    private final DeleteReceiptUseCase deleteUseCase;

    private final MutableLiveData<List<Receipt>> receipts = new MutableLiveData<>();
    private final MutableLiveData<List<ReceiptItemSummary>> allItems = new MutableLiveData<>();
    private final MutableLiveData<ViewMode> viewMode = new MutableLiveData<>(ViewMode.RECEIPTS);
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);

    public HistoryViewModel(GetReceiptsPagedUseCase getReceiptsPagedUseCase, 
                            GetAllItemsPagedUseCase getAllItemsPagedUseCase, 
                            DeleteReceiptUseCase deleteUseCase) {
        this.getReceiptsPagedUseCase = getReceiptsPagedUseCase;
        this.getAllItemsPagedUseCase = getAllItemsPagedUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    public LiveData<List<Receipt>> getReceipts() { return receipts; }
    public LiveData<List<ReceiptItemSummary>> getAllItems() { return allItems; }
    public LiveData<ViewMode> getViewMode() { return viewMode; }
    public LiveData<Integer> getCurrentPage() { return currentPage; }

    public void setViewMode(ViewMode mode) {
        viewMode.setValue(mode);
        currentPage.setValue(0);
        loadData();
    }

    public void nextPage() {
        currentPage.setValue(currentPage.getValue() + 1);
        loadData();
    }

    public void prevPage() {
        if (currentPage.getValue() > 0) {
            currentPage.setValue(currentPage.getValue() - 1);
            loadData();
        }
    }

    public void loadData() {
        isLoading.setValue(true);
        int page = currentPage.getValue();
        ViewMode mode = viewMode.getValue();

        new Thread(() -> {
            try {
                if (mode == ViewMode.RECEIPTS) {
                    List<Receipt> list = getReceiptsPagedUseCase.execute(page, 10);
                    receipts.postValue(list);
                } else {
                    List<ReceiptItemSummary> list = getAllItemsPagedUseCase.execute(page, 25);
                    allItems.postValue(list);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to load data", e);
                errorMessages.postValue("Failed to load history: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        }).start();
    }

    /**
     * 刪除指定收據
     */
    public void deleteReceipt(String receiptId) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Requesting deletion of receipt: " + receiptId);
                deleteUseCase.execute(receiptId);
                Log.d(TAG, "Deletion confirmed, reloading data");
                loadData();
            } catch (Exception e) {
                Log.e(TAG, "Delete failed in ViewModel", e);
                errorMessages.postValue("Delete failed: " + e.getMessage());
            }
        }).start();
    }
}
