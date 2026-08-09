package com.example.nzreceiptapp.domain.logic;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;

import java.util.Map;

/**
 * 基於關鍵字規則庫的二級分類識別器
 */
public class CategoryClassifier {
    private final ICategoryRepository repository;
    private Map<String, Category> rulesCache;

    public CategoryClassifier(ICategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 核心識別邏輯
     */
    public Category classify(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return null;
        }

        // 懶加載規則庫
        if (rulesCache == null) {
            rulesCache = repository.getAllClassificationRules();
        }

        String cleanedName = " " + normalizeForMatching(cleanItemName(itemName)) + " ";
        Category bestMatch = null;
        int longestKeywordLength = 0;

        // 長度優先匹配 (Longest Match Wins)
        for (Map.Entry<String, Category> entry : rulesCache.entrySet()) {
            String keyword = normalizeForMatching(entry.getKey());
            if (cleanedName.contains(" " + keyword + " ")) {
                if (keyword.length() > longestKeywordLength) {
                    longestKeywordLength = keyword.length();
                    bestMatch = entry.getValue();
                }
            }
        }

        return bestMatch;
    }

    /**
     * 名稱清洗：移除單位規格與特殊符號，提升匹配率
     */
    private String cleanItemName(String name) {
        // 移除常見單位：2L, 500g, 1kg 等
        return name.replaceAll("(?i)\\d+\\s*(L|ml|g|kg|PK|EA|pack)", "")
                   .replaceAll("[\\^\\*#]", "")
                   .trim();
    }

    private String normalizeForMatching(String value) {
        return value.toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
