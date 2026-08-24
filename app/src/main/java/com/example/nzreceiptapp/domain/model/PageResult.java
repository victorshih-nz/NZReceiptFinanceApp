package com.example.nzreceiptapp.domain.model;

import java.util.List;

public class PageResult<T> {
    public final List<T> items;
    public final int totalCount;

    public PageResult(List<T> items, int totalCount) {
        this.items = items;
        this.totalCount = totalCount;
    }
}
