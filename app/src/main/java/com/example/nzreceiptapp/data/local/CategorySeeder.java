package com.example.nzreceiptapp.data.local;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;

import java.util.UUID;

public class CategorySeeder {

    public static void seedFromText(String content, ICategoryRepository repository) {
        String[] lines = content.split("\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\t");
            if (parts.length < 3) continue;

            String keyword = parts[0].trim().toLowerCase();
            String parentName = parts[1].trim();
            String subName = parts[2].trim();

            // 1. 處理大類
            Category parent = repository.findCategoryByName(parentName, null);
            if (parent == null) {
                parent = new Category(UUID.randomUUID().toString(), parentName, null);
                repository.saveCategory(parent);
            }

            // 2. 處理子類
            Category sub = repository.findCategoryByName(subName, parentName);
            if (sub == null) {
                sub = new Category(UUID.randomUUID().toString(), subName, parent);
                repository.saveCategory(sub);
            }

            // 3. 儲存規則
            repository.saveRule(keyword, sub.getId());
        }
    }
}
