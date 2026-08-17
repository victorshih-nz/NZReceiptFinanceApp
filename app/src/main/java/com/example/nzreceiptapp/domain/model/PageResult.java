package com.example.nzreceiptapp.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable content and metadata for one valid, one-based page.
 */
public final class PageResult<T> {
    private final List<T> items;
    private final int currentPage;
    private final int pageSize;
    private final int totalRecords;
    private final int totalPages;

    public PageResult(List<T> items, int currentPage, int pageSize, int totalRecords) {
        Objects.requireNonNull(items, "items must not be null");
        if (currentPage < 1) {
            throw new IllegalArgumentException("currentPage must be at least 1");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }
        if (totalRecords < 0) {
            throw new IllegalArgumentException("totalRecords must not be negative");
        }

        int calculatedTotalPages = totalRecords == 0
                ? 1
                : ((totalRecords - 1) / pageSize) + 1;
        if (currentPage > calculatedTotalPages) {
            throw new IllegalArgumentException("currentPage must not exceed totalPages");
        }

        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalRecords = totalRecords;
        this.totalPages = calculatedTotalPages;
    }

    public List<T> getItems() {
        return items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasPrevious() {
        return currentPage > 1;
    }

    public boolean hasNext() {
        return totalRecords > 0 && currentPage < totalPages;
    }
}
