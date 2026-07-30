package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.util.List;

public class GetAllItemsPagedUseCase {
    private final IReceiptRepository repository;

    public GetAllItemsPagedUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public List<ReceiptItemSummary> execute(int page, int pageSize) {
        int offset = page * pageSize;
        return repository.getAllItemsPaged(pageSize, offset);
    }
}
