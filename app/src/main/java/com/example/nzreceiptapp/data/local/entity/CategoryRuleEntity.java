package com.example.nzreceiptapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "category_rules",
        foreignKeys = @ForeignKey(entity = CategoryEntity.class,
                parentColumns = "id",
                childColumns = "category_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "keyword", unique = true)})
public class CategoryRuleEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "keyword")
    @NonNull
    public String keyword;

    @ColumnInfo(name = "category_id")
    @NonNull
    public String categoryId;

    public CategoryRuleEntity(@NonNull String keyword, @NonNull String categoryId) {
        this.keyword = keyword;
        this.categoryId = categoryId;
    }
}
