package com.example.nzreceiptapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.nzreceiptapp.data.local.entity.CategoryEntity;
import com.example.nzreceiptapp.data.local.entity.CategoryRuleEntity;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRule(CategoryRuleEntity rule);

    @Query("SELECT * FROM categories")
    List<CategoryEntity> getAllCategories();

    @Query("SELECT * FROM category_rules")
    List<CategoryRuleEntity> getAllRules();

    @Query("SELECT * FROM categories WHERE name = :name AND parent_id IS NULL LIMIT 1")
    CategoryEntity getParentCategoryByName(String name);

    @Query("SELECT * FROM categories WHERE name = :name AND parent_id = :parentId LIMIT 1")
    CategoryEntity getSubCategoryByName(String name, String parentId);

    @Transaction
    @Query("DELETE FROM categories")
    void deleteAll();
}
