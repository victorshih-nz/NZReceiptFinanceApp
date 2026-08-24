package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.util.List;

import com.example.nzreceiptapp.domain.model.PageResult;

public class GetReceiptsPagedUseCase {
    private final IReceiptRepository repository;

    public GetReceiptsPagedUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public List<Receipt> execute(int page, int pageSize) {
        int offset = page * pageSize;
        return repository.getReceiptsPaged(pageSize, offset);
    }

    public PageResult<Receipt> executeWithCount(int page, int pageSize) {
        int offset = page * pageSize;
        List<Receipt> items = repository.getReceiptsPaged(pageSize, offset);
        int total = repository.getReceiptsCount();
        return new PageResult<>(items, total);
    }
}
