package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;

import java.util.List;

public final class GetCategoriesUseCase {
    private final ICategoryRepository repository;

    public GetCategoriesUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    public List<Category> execute() {
        return repository.getAllCategories();
    }
}
