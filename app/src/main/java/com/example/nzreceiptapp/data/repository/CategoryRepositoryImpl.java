package com.example.nzreceiptapp.data.repository;

import com.example.nzreceiptapp.data.local.dao.CategoryDao;
import com.example.nzreceiptapp.data.local.entity.CategoryEntity;
import com.example.nzreceiptapp.data.local.entity.CategoryRuleEntity;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryRepositoryImpl implements ICategoryRepository {
    private final CategoryDao categoryDao;

    public CategoryRepositoryImpl(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Override
    public Map<String, Category> getAllClassificationRules() {
        List<CategoryEntity> entities = categoryDao.getAllCategories();
        List<CategoryRuleEntity> rules = categoryDao.getAllRules();

        // 建立 ID 到 Domain Model 的 Mapping
        Map<String, Category> idMap = new HashMap<>();
        
        // 1. 先處理大類 (Parent is null)
        for (CategoryEntity entity : entities) {
            if (entity.parentId == null) {
                idMap.put(entity.id, new Category(entity.id, entity.name, null));
            }
        }
        
        // 2. 處理子類
        for (CategoryEntity entity : entities) {
            if (entity.parentId != null) {
                Category parent = idMap.get(entity.parentId);
                idMap.put(entity.id, new Category(entity.id, entity.name, parent));
            }
        }

        // 3. 組裝規則
        Map<String, Category> result = new HashMap<>();
        for (CategoryRuleEntity rule : rules) {
            result.put(rule.keyword, idMap.get(rule.categoryId));
        }
        return result;
    }

    @Override
    public Category findCategoryByName(String name, String parentName) {
        if (parentName == null) {
            CategoryEntity entity = categoryDao.getParentCategoryByName(name);
            return entity != null ? new Category(entity.id, entity.name, null) : null;
        } else {
            Category parent = findCategoryByName(parentName, null);
            if (parent == null) return null;
            CategoryEntity entity = categoryDao.getSubCategoryByName(name, parent.getId());
            return entity != null ? new Category(entity.id, entity.name, parent) : null;
        }
    }

    @Override
    public void saveCategory(Category category) {
        String parentId = category.getParentCategory() != null ? category.getParentCategory().getId() : null;
        categoryDao.insertCategory(new CategoryEntity(category.getId(), category.getName(), parentId));
    }

    @Override
    public void saveRule(String keyword, String categoryId) {
        categoryDao.insertRule(new CategoryRuleEntity(keyword, categoryId));
    }
}
