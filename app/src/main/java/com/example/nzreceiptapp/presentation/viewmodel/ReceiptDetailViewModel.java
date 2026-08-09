package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;
import com.example.nzreceiptapp.presentation.base.BaseViewModel;

import java.util.concurrent.Executor;

/** Owns receipt-detail state and keeps data access out of the Fragment. */
public final class ReceiptDetailViewModel extends BaseViewModel {
    private final GetReceiptByIdUseCase getReceiptByIdUseCase;
    private final Executor ioExecutor;
    private final MutableLiveData<Receipt> receipt = new MutableLiveData<>();

    public ReceiptDetailViewModel(GetReceiptByIdUseCase getReceiptByIdUseCase,
                                  Executor ioExecutor) {
        this.getReceiptByIdUseCase = getReceiptByIdUseCase;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<Receipt> getReceipt() {
        return receipt;
    }

    public void loadReceipt(String receiptId) {
        if (receiptId == null || receiptId.trim().isEmpty()) {
            errorMessages.setValue("Receipt ID is missing");
            return;
        }

        isLoading.setValue(true);
        ioExecutor.execute(() -> {
            try {
                Receipt result = getReceiptByIdUseCase.execute(receiptId);
                if (result == null) {
                    errorMessages.postValue("Receipt not found");
                } else {
                    receipt.postValue(result);
                }
            } catch (Exception exception) {
                errorMessages.postValue("Failed to load receipt: " + exception.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
}
