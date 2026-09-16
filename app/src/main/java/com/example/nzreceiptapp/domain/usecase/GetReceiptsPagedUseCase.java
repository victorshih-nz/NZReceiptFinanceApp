package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

public class GetReceiptsPagedUseCase {
    private final IReceiptRepository repository;

    public GetReceiptsPagedUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public PageResult<Receipt> execute(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be at least 1");
        }
        if (!isSupportedPageSize(pageSize)) {
            throw new IllegalArgumentException("pageSize must be 15, 30, or 50");
        }
        return repository.getReceiptsPage(page, pageSize);
    }

    private boolean isSupportedPageSize(int pageSize) {
        return pageSize == 15 || pageSize == 30 || pageSize == 50;
    }
}
