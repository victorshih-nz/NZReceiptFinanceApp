package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

public class GetReceiptByIdUseCase {
    private final IReceiptRepository repository;

    public GetReceiptByIdUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public Receipt execute(String id) {
        return repository.getReceiptById(id);
    }
}
