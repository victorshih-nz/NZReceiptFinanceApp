package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.service.IReceiptImageStore;
import com.example.nzreceiptapp.domain.usecase.GetCategoriesUseCase;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** Coordinates capture, OCR, parsing, review and explicit saving. */
public final class ScannerViewModel extends ViewModel {
    private final IOCRService ocrService;
    private final ParseReceiptUseCase parseUseCase;
    private final SaveReceiptUseCase saveUseCase;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final IReceiptImageStore imageStore;
    private final Executor ioExecutor;

    private final MutableLiveData<ScannerUiState> uiState =
            new MutableLiveData<>(ScannerUiState.idle());

    public ScannerViewModel(IOCRService ocrService,
                            ParseReceiptUseCase parseUseCase,
                            SaveReceiptUseCase saveUseCase,
                            GetCategoriesUseCase getCategoriesUseCase,
                            IReceiptImageStore imageStore,
                            Executor ioExecutor) {
        this.ocrService = ocrService;
        this.parseUseCase = parseUseCase;
        this.saveUseCase = saveUseCase;
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.imageStore = imageStore;
        this.ioExecutor = ioExecutor;
    }

    public ScannerViewModel(IOCRService ocrService,
                            ParseReceiptUseCase parseUseCase,
                            SaveReceiptUseCase saveUseCase,
                            Executor ioExecutor) {
        this(ocrService, parseUseCase, saveUseCase, null, new IReceiptImageStore() {
            @Override public String persist(String sourceUri) { return sourceUri; }
            @Override public void delete(String storedUri) { }
        }, ioExecutor);
    }

    public LiveData<ScannerUiState> getUiState() {
        return uiState;
    }

    /** OCR and parse only. Saving happens after the user reviews the draft. */
    public void processReceiptImage(String imagePath, String chainName, String branchName) {
        uiState.setValue(ScannerUiState.working(ScannerUiState.Phase.EXTRACTING_TEXT));
        ioExecutor.execute(() -> {
            try {
                String storedImageUri = imageStore.persist(imagePath);
                extractText(storedImageUri, chainName, branchName);
            } catch (Exception exception) {
                uiState.postValue(ScannerUiState.error(
                        null, null, "Preparing image failed: " + safeMessage(exception)));
            }
        });
    }

    private void extractText(String storedImageUri, String chainName, String branchName) {
        ocrService.extractText(storedImageUri, new IOCRService.OnOCRCompleteListener() {
            @Override
            public void onSuccess(String text) {
                parseDraft(text, storedImageUri, chainName, branchName);
            }

            @Override
            public void onFailure(Exception exception) {
                imageStore.delete(storedImageUri);
                uiState.postValue(ScannerUiState.error(
                        null, null, "OCR failed: " + safeMessage(exception)));
            }
        });
    }

    private void parseDraft(String rawText, String imagePath,
                            String chainName, String branchName) {
        ioExecutor.execute(() -> {
            uiState.postValue(ScannerUiState.working(ScannerUiState.Phase.PARSING_RECEIPT));
            try {
                Receipt receipt = parseUseCase.execute(
                        rawText, chainName, branchName, LocalDateTime.now(), imagePath);
                List<Category> categories = loadSubCategories();
                uiState.postValue(ScannerUiState.ready(receipt, categories));
            } catch (Exception exception) {
                imageStore.delete(imagePath);
                uiState.postValue(ScannerUiState.error(
                        null, null, "Parsing failed: " + safeMessage(exception)));
            }
        });
    }

    public void saveReviewedReceipt(String chainName, String branchName,
                                    List<ReceiptItem> editedItems) {
        ScannerUiState current = uiState.getValue();
        if (current == null || current.getDraft() == null) {
            uiState.setValue(ScannerUiState.error(null, null, "Receipt draft is missing"));
            return;
        }

        Receipt editedReceipt;
        try {
            editedReceipt = createEditedReceipt(
                    current.getDraft(), chainName, branchName, editedItems);
        } catch (IllegalArgumentException exception) {
            uiState.setValue(ScannerUiState.error(
                    current.getDraft(), current.getCategories(), exception.getMessage()));
            return;
        }

        List<Category> categories = current.getCategories();
        uiState.setValue(ScannerUiState.saving(editedReceipt, categories));
        ioExecutor.execute(() -> {
            try {
                saveUseCase.execute(editedReceipt);
                uiState.postValue(ScannerUiState.saved(editedReceipt, categories));
            } catch (Exception exception) {
                uiState.postValue(ScannerUiState.error(
                        editedReceipt, categories, "Saving failed: " + safeMessage(exception)));
            }
        });
    }

    public void reset() {
        uiState.setValue(ScannerUiState.idle());
    }

    public void discardDraft() {
        ScannerUiState current = uiState.getValue();
        Receipt draft = current == null ? null : current.getDraft();
        uiState.setValue(ScannerUiState.idle());
        if (draft != null && draft.getImageUri() != null) {
            ioExecutor.execute(() -> imageStore.delete(draft.getImageUri()));
        }
    }

    private Receipt createEditedReceipt(Receipt draft, String chainName,
                                        String branchName, List<ReceiptItem> items) {
        String cleanChain = chainName == null ? "" : chainName.trim();
        String cleanBranch = branchName == null ? "" : branchName.trim();
        if (cleanChain.isEmpty()) throw new IllegalArgumentException("Store chain is required");
        if (cleanBranch.isEmpty()) throw new IllegalArgumentException("Branch is required");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("At least one receipt item is required");
        }

        Store store = new Store(draft.getStore().getId(), cleanChain, cleanBranch);
        return new Receipt(
                draft.getId(),
                store,
                new ArrayList<>(items),
                draft.getPurchaseDate(),
                draft.getTotalDiscountCents(),
                draft.isSynced(),
                draft.getRawOcrText(),
                draft.getImageUri(),
                draft.getPrintedTotalCents()
        );
    }

    private List<Category> loadSubCategories() {
        if (getCategoriesUseCase == null) return Collections.emptyList();
        List<Category> result = new ArrayList<>();
        for (Category category : getCategoriesUseCase.execute()) {
            if (category.isSubCategory()) result.add(category);
        }
        return result;
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
