package com.example.nzreceiptapp.domain.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CategoryClassifierTest {

    private CategoryClassifier classifier;
    private MockCategoryRepository mockRepository;

    @Before
    public void setUp() {
        mockRepository = new MockCategoryRepository();
        classifier = new CategoryClassifier(mockRepository);

        // 初始化測試數據
        Category grocery = new Category(UUID.randomUUID().toString(), "Grocery", null);
        Category freshFood = new Category(UUID.randomUUID().toString(), "Fresh Food", grocery);
        Category snacks = new Category(UUID.randomUUID().toString(), "Snacks & Beverages", grocery);

        mockRepository.addRule("Milk", freshFood);
        mockRepository.addRule("Eggs", freshFood);
        mockRepository.addRule("Coca Cola", snacks);
        mockRepository.addRule("Chocolate", snacks);
    }

    @Test
    public void testClassify_StandardMatch() {
        Category result = classifier.classify("WW Milk Standard 3L");
        assertNotNull(result);
        assertEquals("Fresh Food", result.getName());
        assertEquals("Grocery", result.getParentCategory().getName());
    }

    @Test
    public void testClassify_LongestMatch() {
        // 增加一個更長且包含 Milk 的規則
        Category specialMilk = new Category(UUID.randomUUID().toString(), "Special Milk", null);
        mockRepository.addRule("Standard Milk", specialMilk);

        Category result = classifier.classify("WW Standard Milk 3L");
        assertNotNull(result);
        assertEquals("Special Milk", result.getName());
    }

    @Test
    public void testClassify_CleaningLogic() {
        // 測試移除單位後是否還能匹配
        Category result = classifier.classify("^ Coca Cola 2.25L #");
        assertNotNull(result);
        assertEquals("Snacks & Beverages", result.getName());
    }

    @Test
    public void testClassify_DoesNotMatchInsideAnotherWord() {
        Category eggCategory = new Category("egg", "Eggs", null);
        mockRepository.addRule("egg", eggCategory);

        assertNull(classifier.classify("Veggie burger"));
    }

    // 簡易 Mock 實作
    private static class MockCategoryRepository implements ICategoryRepository {
        private final Map<String, Category> rules = new HashMap<>();

        public void addRule(String keyword, Category category) {
            rules.put(keyword, category);
        }

        @Override
        public Map<String, Category> getAllClassificationRules() {
            return rules;
        }

        @Override
        public Category findCategoryByName(String name, String parentName) { return null; }
        @Override
        public void saveCategory(Category category) {}
        @Override
        public void saveRule(String keyword, String categoryId) {}
    }
}
