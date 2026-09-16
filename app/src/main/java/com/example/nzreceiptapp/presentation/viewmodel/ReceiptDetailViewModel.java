package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;
import java.util.concurrent.Executor;

/** Owns receipt-detail state and keeps data access out of the Fragment. */
public final class ReceiptDetailViewModel extends ViewModel {
    private final GetReceiptByIdUseCase getReceiptByIdUseCase;
    private final Executor ioExecutor;
    private final MutableLiveData<ReceiptDetailUiState> uiState =
            new MutableLiveData<>(ReceiptDetailUiState.initial());
    private String receiptId;

    public ReceiptDetailViewModel(GetReceiptByIdUseCase getReceiptByIdUseCase,
                                  Executor ioExecutor) {
        this.getReceiptByIdUseCase = getReceiptByIdUseCase;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<ReceiptDetailUiState> getUiState() {
        return uiState;
    }

    public void loadReceipt(String receiptId) {
        if (receiptId == null || receiptId.trim().isEmpty()) {
            this.receiptId = null;
            uiState.setValue(ReceiptDetailUiState.error("Receipt ID is missing"));
            return;
        }

        ReceiptDetailUiState current = uiState.getValue();
        if (receiptId.equals(this.receiptId)
                && current != null
                && (current.getLoadState() == ReceiptDetailUiState.LoadState.LOADING
                || current.getLoadState() == ReceiptDetailUiState.LoadState.CONTENT)) {
            return;
        }

        this.receiptId = receiptId;
        loadCurrentReceipt();
    }

    public void retry() {
        ReceiptDetailUiState current = uiState.getValue();
        if (receiptId == null
                || current == null
                || current.getLoadState() != ReceiptDetailUiState.LoadState.ERROR) {
            return;
        }
        loadCurrentReceipt();
    }

    private void loadCurrentReceipt() {
        String requestedId = receiptId;
        uiState.setValue(ReceiptDetailUiState.loading());
        ioExecutor.execute(() -> {
            try {
                Receipt result = getReceiptByIdUseCase.execute(requestedId);
                if (result == null) {
                    uiState.postValue(ReceiptDetailUiState.error("Receipt not found"));
                } else {
                    uiState.postValue(ReceiptDetailUiState.content(result));
                }
            } catch (Exception exception) {
                uiState.postValue(ReceiptDetailUiState.error(
                        "Failed to load receipt: " + safeMessage(exception)));
            }
        });
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
