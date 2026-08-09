package com.example.nzreceiptapp.domain.repository;

import com.example.nzreceiptapp.domain.model.Category;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ICategoryRepository {
    /**
     * 獲取所有分類規則，Key 是關鍵字，Value 是對應的二級分類
     */
    Map<String, Category> getAllClassificationRules();

    /**
     * 根據名稱尋找分類
     */
    Category findCategoryByName(String name, String parentName);

    /** Returns all categories with their parent relationship restored. */
    default List<Category> getAllCategories() {
        return Collections.emptyList();
    }
    
    /**
     * 儲存規則 (供初期 Seed 使用)
     */
    void saveCategory(Category category);
    void saveRule(String keyword, String categoryId);
}
