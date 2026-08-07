package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;
import com.example.nzreceiptapp.presentation.base.BaseViewModel;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * 處理收據掃描流程的 ViewModel
 */
public class ScannerViewModel extends BaseViewModel {

    public enum State { IDLE, EXTRACTING_TEXT, PARSING_RECEIPT, SAVING_RECEIPT, SUCCESS, ERROR }

    private final IOCRService ocrService;
    private final ParseReceiptUseCase parseUseCase;
    private final SaveReceiptUseCase saveUseCase;
    private final Executor ioExecutor;

    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<Receipt> lastProcessedReceipt = new MutableLiveData<>();

    public ScannerViewModel(IOCRService ocrService, ParseReceiptUseCase parseUseCase,
                            SaveReceiptUseCase saveUseCase, Executor ioExecutor) {
        this.ocrService = ocrService;
        this.parseUseCase = parseUseCase;
        this.saveUseCase = saveUseCase;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<State> getState() { return state; }
    public LiveData<Receipt> getLastProcessedReceipt() { return lastProcessedReceipt; }

    /**
     * 開始完整的掃描流程：OCR -> 解析 -> 儲存
     */
    public void processReceiptImage(String imagePath, String chainName, String branchName) {
        state.setValue(State.EXTRACTING_TEXT);
        isLoading.setValue(true);

        ocrService.extractText(imagePath, new IOCRService.OnOCRCompleteListener() {
            @Override
            public void onSuccess(String text) {
                parseAndSave(text, chainName, branchName);
            }

            @Override
            public void onFailure(Exception e) {
                handleError("OCR failed: " + e.getMessage());
            }
        });
    }

    private void parseAndSave(String rawText, String chainName, String branchName) {
        ioExecutor.execute(() -> {
            state.postValue(State.PARSING_RECEIPT);
            try {
                // 執行解析 (Domain Logic)
                Receipt receipt = parseUseCase.execute(rawText, chainName, branchName, LocalDateTime.now());

                state.postValue(State.SAVING_RECEIPT);
                // 執行儲存 (Domain Logic)
                saveUseCase.execute(receipt);

                lastProcessedReceipt.postValue(receipt);
                state.postValue(State.SUCCESS);
                isLoading.postValue(false);
            } catch (Exception e) {
                handleError("Parsing/Saving failed: " + e.getMessage());
            }
        });
    }

    private void handleError(String message) {
        errorMessages.postValue(message);
        state.postValue(State.ERROR);
        isLoading.postValue(false);
    }
}
