package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.util.List;

/**
 * 獲取所有收據歷史紀錄的 Use Case
 */
public class GetReceiptsUseCase {
    private final IReceiptRepository repository;

    public GetReceiptsUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public List<Receipt> execute() {
        return repository.getAllReceipts();
    }
}
