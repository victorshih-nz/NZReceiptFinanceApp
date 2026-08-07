package com.example.nzreceiptapp.presentation.viewmodel;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.Receipt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One immutable source of truth for the complete receipt-capture workflow. */
public final class ScannerUiState {
    public enum Phase {
        IDLE,
        EXTRACTING_TEXT,
        PARSING_RECEIPT,
        READY_FOR_REVIEW,
        SAVING_RECEIPT,
        SAVED,
        ERROR
    }

    private final Phase phase;
    private final Receipt draft;
    private final List<Category> categories;
    private final String errorMessage;

    private ScannerUiState(Phase phase, Receipt draft,
                           List<Category> categories, String errorMessage) {
        this.phase = phase;
        this.draft = draft;
        this.categories = categories == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(categories));
        this.errorMessage = errorMessage;
    }

    public static ScannerUiState idle() {
        return new ScannerUiState(Phase.IDLE, null, null, null);
    }

    public static ScannerUiState working(Phase phase) {
        return new ScannerUiState(phase, null, null, null);
    }

    public static ScannerUiState ready(Receipt draft, List<Category> categories) {
        return new ScannerUiState(Phase.READY_FOR_REVIEW, draft, categories, null);
    }

    public static ScannerUiState saving(Receipt draft, List<Category> categories) {
        return new ScannerUiState(Phase.SAVING_RECEIPT, draft, categories, null);
    }

    public static ScannerUiState saved(Receipt receipt, List<Category> categories) {
        return new ScannerUiState(Phase.SAVED, receipt, categories, null);
    }

    public static ScannerUiState error(Receipt draft, List<Category> categories,
                                       String errorMessage) {
        return new ScannerUiState(Phase.ERROR, draft, categories, errorMessage);
    }

    public Phase getPhase() { return phase; }
    public Receipt getDraft() { return draft; }
    public List<Category> getCategories() { return categories; }
    public String getErrorMessage() { return errorMessage; }

    public boolean isLoading() {
        return phase == Phase.EXTRACTING_TEXT
                || phase == Phase.PARSING_RECEIPT
                || phase == Phase.SAVING_RECEIPT;
    }

    public boolean canReview() {
        return draft != null && (phase == Phase.READY_FOR_REVIEW || phase == Phase.ERROR);
    }
}
