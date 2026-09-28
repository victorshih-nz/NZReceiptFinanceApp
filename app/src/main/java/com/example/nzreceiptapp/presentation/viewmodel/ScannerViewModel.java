package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nzreceiptapp.domain.logic.ReceiptValidator;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.service.IReceiptImagePreprocessor;
import com.example.nzreceiptapp.domain.service.IReceiptImageStore;
import com.example.nzreceiptapp.domain.service.ReceiptCropProposal;
import com.example.nzreceiptapp.domain.usecase.GetCategoriesUseCase;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public final class ScannerViewModel extends ViewModel {
    private final IOCRService ocrService;
    private final ParseReceiptUseCase parseUseCase;
    private final SaveReceiptUseCase saveUseCase;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final IReceiptImageStore imageStore;
    private final IReceiptImagePreprocessor imagePreprocessor;
    private final Executor ioExecutor;
    private final boolean cropReviewRequired;

    private final MutableLiveData<ScannerUiState> uiState =
            new MutableLiveData<>(ScannerUiState.idle());
    private final MutableLiveData<CropReviewState> cropReviewState =
            new MutableLiveData<>(CropReviewState.idle());

    private PendingImage pending;

    public ScannerViewModel(IOCRService ocrService,
                            ParseReceiptUseCase parseUseCase,
                            SaveReceiptUseCase saveUseCase,
                            GetCategoriesUseCase getCategoriesUseCase,
                            IReceiptImageStore imageStore,
                            IReceiptImagePreprocessor imagePreprocessor,
                            Executor ioExecutor) {
        this(ocrService, parseUseCase, saveUseCase, getCategoriesUseCase,
                imageStore, imagePreprocessor, ioExecutor, true);
    }

    private ScannerViewModel(IOCRService ocrService,
                             ParseReceiptUseCase parseUseCase,
                             SaveReceiptUseCase saveUseCase,
                             GetCategoriesUseCase getCategoriesUseCase,
                             IReceiptImageStore imageStore,
                             IReceiptImagePreprocessor imagePreprocessor,
                             Executor ioExecutor,
                             boolean cropReviewRequired) {
        this.ocrService = ocrService;
        this.parseUseCase = parseUseCase;
        this.saveUseCase = saveUseCase;
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.imageStore = imageStore;
        this.imagePreprocessor = imagePreprocessor;
        this.ioExecutor = ioExecutor;
        this.cropReviewRequired = cropReviewRequired;
    }

    public ScannerViewModel(IOCRService ocrService,
                            ParseReceiptUseCase parseUseCase,
                            SaveReceiptUseCase saveUseCase,
                            Executor ioExecutor) {
        this(ocrService, parseUseCase, saveUseCase, null,
                new IReceiptImageStore() {
                    @Override public String persist(String sourceUri) { return sourceUri; }
                    @Override public void delete(String storedUri) { }
                },
                new IReceiptImagePreprocessor() {
                    @Override public ReceiptCropProposal analyze(String uri) {
                        return ReceiptCropProposal.fullImage(uri, "test");
                    }
                    @Override public Result crop(String uri,
                            List<ReceiptCropProposal.Point> corners) {
                        return Result.passThrough(uri);
                    }
                    @Override public void cleanup(Result result) { }
                }, ioExecutor, false);
    }

    public LiveData<ScannerUiState> getUiState() { return uiState; }
    public LiveData<CropReviewState> getCropReviewState() { return cropReviewState; }

    public void processReceiptImage(String imagePath,
                                    String chainName,
                                    String branchName) {
        if (pending != null) return;
        cropReviewState.setValue(CropReviewState.preparing());

        ioExecutor.execute(() -> {
            String original = null;
            try {
                original = imageStore.persist(imagePath);
                ReceiptCropProposal proposal = imagePreprocessor.analyze(original);
                PendingImage prepared =
                        new PendingImage(original, chainName, branchName);
                pending = prepared;

                if (cropReviewRequired) {
                    cropReviewState.postValue(CropReviewState.ready(proposal));
                } else {
                    // Backward-compatible path for existing unit tests and
                    // non-UI callers using the legacy constructor.
                    cropReviewState.postValue(CropReviewState.idle());
                    uiState.postValue(ScannerUiState.working(
                            ScannerUiState.Phase.EXTRACTING_TEXT));
                    extractText(
                            IReceiptImagePreprocessor.Result.passThrough(original),
                            prepared);
                }
            } catch (Exception e) {
                if (original != null) imageStore.delete(original);
                pending = null;
                cropReviewState.postValue(CropReviewState.error(
                        null, "Preparing image failed: " + safeMessage(e)));
            }
        });
    }

    public void confirmCrop(List<ReceiptCropProposal.Point> corners) {
        PendingImage p = pending;
        if (p == null) return;

        cropReviewState.setValue(CropReviewState.applying(
                p.originalUri, corners));
        uiState.setValue(ScannerUiState.working(
                ScannerUiState.Phase.EXTRACTING_TEXT));

        ioExecutor.execute(() ->
                extractText(imagePreprocessor.crop(p.originalUri, corners), p));
    }

    public void confirmOriginalImage() {
        PendingImage p = pending;
        if (p == null) return;

        cropReviewState.setValue(CropReviewState.applying(
                p.originalUri, Collections.emptyList()));
        uiState.setValue(ScannerUiState.working(
                ScannerUiState.Phase.EXTRACTING_TEXT));

        extractText(IReceiptImagePreprocessor.Result.passThrough(p.originalUri), p);
    }

    public void cancelCropReview() {
        PendingImage p = pending;
        pending = null;
        cropReviewState.setValue(CropReviewState.idle());
        if (p != null) {
            ioExecutor.execute(() -> imageStore.delete(p.originalUri));
        }
    }

    private void extractText(IReceiptImagePreprocessor.Result prep,
                             PendingImage p) {
        ocrService.extractText(prep.getOcrImageUri(),
                new IOCRService.OnOCRCompleteListener() {
                    @Override public void onSuccess(String text) {
                        imagePreprocessor.cleanup(prep);
                        pending = null;
                        cropReviewState.postValue(CropReviewState.idle());
                        parseDraft(text, p.originalUri, p.chainName, p.branchName);
                    }

                    @Override public void onFailure(Exception e) {
                        imagePreprocessor.cleanup(prep);
                        imageStore.delete(p.originalUri);
                        pending = null;
                        cropReviewState.postValue(CropReviewState.idle());
                        uiState.postValue(ScannerUiState.error(
                                null, null, "OCR failed: " + safeMessage(e)));
                    }
                });
    }

    private void parseDraft(String rawText, String imageUri,
                            String chainName, String branchName) {
        ioExecutor.execute(() -> {
            uiState.postValue(ScannerUiState.working(
                    ScannerUiState.Phase.PARSING_RECEIPT));
            try {
                Receipt receipt = parseUseCase.execute(
                        rawText, chainName, branchName,
                        LocalDateTime.now(), imageUri);
                uiState.postValue(ScannerUiState.ready(
                        receipt, loadSubCategories()));
            } catch (Exception e) {
                imageStore.delete(imageUri);
                uiState.postValue(ScannerUiState.error(
                        null, null, "Parsing failed: " + safeMessage(e)));
            }
        });
    }

    public void saveReviewedReceipt(String chainName, String branchName,
                                    List<ReceiptItem> editedItems) {
        ScannerUiState current = uiState.getValue();
        if (current == null || current.getDraft() == null) {
            uiState.setValue(ScannerUiState.error(
                    null, null, "Receipt draft is missing"));
            return;
        }

        Receipt edited = createEditedReceipt(
                current.getDraft(), chainName, branchName, editedItems);
        List<Category> categories = current.getCategories();
        uiState.setValue(ScannerUiState.saving(edited, categories));

        ioExecutor.execute(() -> {
            try {
                saveUseCase.execute(edited);
                uiState.postValue(ScannerUiState.saved(edited, categories));
            } catch (SaveReceiptUseCase.ReceiptValidationException e) {
                uiState.postValue(ScannerUiState.error(
                        edited, categories,
                        validationMessage(e.getValidationResult())));
            } catch (Exception e) {
                uiState.postValue(ScannerUiState.error(
                        edited, categories, "Saving failed: " + safeMessage(e)));
            }
        });
    }

    public void reset() { uiState.setValue(ScannerUiState.idle()); }

    public void discardDraft() {
        ScannerUiState current = uiState.getValue();
        Receipt draft = current == null ? null : current.getDraft();
        uiState.setValue(ScannerUiState.idle());
        if (draft != null && draft.getImageUri() != null) {
            ioExecutor.execute(() -> imageStore.delete(draft.getImageUri()));
        }
    }

    private Receipt createEditedReceipt(Receipt draft, String chainName,
                                        String branchName,
                                        List<ReceiptItem> items) {
        Store store = new Store(draft.getStore().getId(),
                chainName == null ? "" : chainName.trim(),
                branchName == null ? "" : branchName.trim());
        return new Receipt(draft.getId(), store,
                items == null ? null : new ArrayList<>(items),
                draft.getPurchaseDate(), draft.getTotalDiscountCents(),
                draft.isSynced(), draft.getRawOcrText(),
                draft.getImageUri(), draft.getPrintedTotalCents());
    }

    private List<Category> loadSubCategories() {
        if (getCategoriesUseCase == null) return Collections.emptyList();
        List<Category> result = new ArrayList<>();
        for (Category c : getCategoriesUseCase.execute()) {
            if (c.isSubCategory()) result.add(c);
        }
        return result;
    }

    private String validationMessage(ReceiptValidator.ValidationResult r) {
        int n = r.getItemIndex() + 1;
        switch (r.getErrorCode()) {
            case RECEIPT_REQUIRED: return "Receipt is required";
            case CHAIN_REQUIRED: return "Store chain is required";
            case ITEMS_REQUIRED: return "At least one receipt item is required";
            case ITEM_REQUIRED: return "Item " + n + " is required";
            case ITEM_NAME_REQUIRED: return "Item " + n + " name is required";
            case ITEM_QUANTITY_INVALID: return "Item " + n + " quantity must be greater than zero";
            case ITEM_UNIT_PRICE_NEGATIVE: return "Item " + n + " unit price cannot be negative";
            case ITEM_CATEGORY_INVALID: return "Item " + n + " category must be a child category or Uncategorized";
            case ITEM_FINAL_SUBTOTAL_NEGATIVE: return "Item " + n + " total cannot be negative";
            case RECEIPT_FINAL_TOTAL_NEGATIVE: return "Receipt total cannot be negative";
            default: return "Receipt validation failed";
        }
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static final class PendingImage {
        final String originalUri;
        final String chainName;
        final String branchName;

        PendingImage(String originalUri, String chainName, String branchName) {
            this.originalUri = originalUri;
            this.chainName = chainName;
            this.branchName = branchName;
        }
    }
}
