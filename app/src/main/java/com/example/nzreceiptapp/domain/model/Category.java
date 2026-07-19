package com.example.nzreceiptapp.domain.model;

/**
 * 支援二級分類的樹狀結構 (Domain Entity)
 */
public class Category {
    private final String id;
    private final String name;              // 分類名稱，例如："生鮮食材"、"零食"
    private final Category parentCategory;  // 指向上層大類。若本身即是大類，則為 null

    public Category(String id, String name, Category parentCategory) {
        this.id = id;
        this.name = name;
        this.parentCategory = parentCategory;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Category getParentCategory() { return parentCategory; }
    public boolean isSubCategory() { return parentCategory != null; } // 判斷是否為二級子分類
}
