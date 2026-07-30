package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

/**
 * 刪除收據的 Use Case
 */
public class DeleteReceiptUseCase {
    private final IReceiptRepository repository;

    public DeleteReceiptUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public void execute(String receiptId) {
        repository.deleteReceipt(receiptId);
    }
}
