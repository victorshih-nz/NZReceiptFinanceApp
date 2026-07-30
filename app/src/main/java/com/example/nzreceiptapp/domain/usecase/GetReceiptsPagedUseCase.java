package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.util.List;

public class GetReceiptsPagedUseCase {
    private final IReceiptRepository repository;

    public GetReceiptsPagedUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public List<Receipt> execute(int page, int pageSize) {
        int offset = page * pageSize;
        return repository.getReceiptsPaged(pageSize, offset);
    }
}
