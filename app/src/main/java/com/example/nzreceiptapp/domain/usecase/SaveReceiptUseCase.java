package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

/**
 * 儲存收據到資料庫的 Use Case
 */
public class SaveReceiptUseCase {

    private final IReceiptRepository repository;

    public SaveReceiptUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public void execute(Receipt receipt) {
        if (receipt == null) return;
        repository.saveReceipt(receipt);
    }
}
