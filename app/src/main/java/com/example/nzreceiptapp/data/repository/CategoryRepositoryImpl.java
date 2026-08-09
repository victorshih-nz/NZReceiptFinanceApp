package com.example.nzreceiptapp.data.repository;

import com.example.nzreceiptapp.data.local.dao.CategoryDao;
import com.example.nzreceiptapp.data.local.entity.CategoryEntity;
import com.example.nzreceiptapp.data.local.entity.CategoryRuleEntity;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;

import java.util.ArrayList;
import java.util.Comparator;
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
        Map<String, Category> categoriesById = mapCategories(categoryDao.getAllCategories());
        List<CategoryRuleEntity> rules = categoryDao.getAllRules();
        Map<String, Category> result = new HashMap<>();
        for (CategoryRuleEntity rule : rules) {
            result.put(rule.keyword, categoriesById.get(rule.categoryId));
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
    public List<Category> getAllCategories() {
        Map<String, Category> idMap = mapCategories(categoryDao.getAllCategories());
        List<Category> result = new ArrayList<>(idMap.values());
        result.sort(Comparator
                .comparing((Category category) -> category.getParentCategory() == null
                        ? category.getName()
                        : category.getParentCategory().getName())
                .thenComparing(Category::getName));
        return result;
    }

    @Override
    public void saveCategory(Category category) {
        String parentId = category.getParentCategory() != null ? category.getParentCategory().getId() : null;
        categoryDao.insertCategory(new CategoryEntity(category.getId(), category.getName(), parentId));
    }

    @Override
    public void saveRule(String keyword, String categoryId) {
        categoryDao.insertRule(new CategoryRuleEntity(keyword.toLowerCase(), categoryId));
    }

    private Map<String, Category> mapCategories(List<CategoryEntity> entities) {
        Map<String, Category> idMap = new HashMap<>();
        for (CategoryEntity entity : entities) {
            if (entity.parentId == null) {
                idMap.put(entity.id, new Category(entity.id, entity.name, null));
            }
        }
        for (CategoryEntity entity : entities) {
            if (entity.parentId != null) {
                Category parent = idMap.get(entity.parentId);
                idMap.put(entity.id, new Category(entity.id, entity.name, parent));
            }
        }
        return idMap;
    }
}
