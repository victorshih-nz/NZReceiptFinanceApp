package com.example.nzreceiptapp.presentation.viewmodel;

import com.example.nzreceiptapp.domain.model.Receipt;

/** Immutable snapshot rendered by the Receipt Detail screen. */
public final class ReceiptDetailUiState {
    public enum LoadState {
        IDLE,
        LOADING,
        CONTENT,
        ERROR
    }

    private final LoadState loadState;
    private final Receipt receipt;
    private final String errorMessage;

    private ReceiptDetailUiState(LoadState loadState,
                                 Receipt receipt,
                                 String errorMessage) {
        this.loadState = loadState;
        this.receipt = receipt;
        this.errorMessage = errorMessage;
    }

    public static ReceiptDetailUiState initial() {
        return new ReceiptDetailUiState(LoadState.IDLE, null, null);
    }

    public static ReceiptDetailUiState loading() {
        return new ReceiptDetailUiState(LoadState.LOADING, null, null);
    }

    public static ReceiptDetailUiState content(Receipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("Receipt is required");
        }
        return new ReceiptDetailUiState(LoadState.CONTENT, receipt, null);
    }

    public static ReceiptDetailUiState error(String message) {
        return new ReceiptDetailUiState(LoadState.ERROR, null, message);
    }

    public LoadState getLoadState() {
        return loadState;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
