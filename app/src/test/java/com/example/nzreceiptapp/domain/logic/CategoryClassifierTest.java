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

/**
 * Category matching contract.
 *
 * The product/item name drives classification. Units/package sizes are merely
 * removed as noise and never classify an item by themselves.
 */
public class CategoryClassifierTest {

    private CategoryClassifier classifier;
    private MockCategoryRepository repository;

    @Before
    public void setUp() {
        repository = new MockCategoryRepository();
        classifier = new CategoryClassifier(repository);

        add("milk", "Food", "Dairy & Eggs");
        add("milk powder", "Food", "Dairy & Eggs");
        add("corn chips", "Food", "Snacks");
        add("seaweed snack", "Food", "Snacks");
        add("coffee", "Food", "Drinks");
        add("sushi rice", "Food", "Pantry");
        add("white toast", "Food", "Bakery");
        add("beef schnitzel", "Food", "Meat");
        add("ranch dressing", "Food", "Condiments & Sauces");
        add("dressing ranch", "Food", "Condiments & Sauces");
        add("paper towel", "Household", "Paper Products");
        add("shampoo", "Personal Care", "Hair Care");
        add("salvation army donation", "Charity", "Donations");
    }

    @Test
    public void classifiesRealReceiptStyleNames() {
        assertCategory("PAMS MILK POWDER WHOLE 400G", "Dairy & Eggs");
        assertCategory("MAMASAN SUSHI RICE 1KG", "Pantry");
        assertCategory("MEXICANO CORN CHIPS THAI SWT CHILLI 170G", "Snacks");
        assertCategory("OFOOD SEAWEED SNACK GREEN 40G", "Snacks");
        assertCategory("MOCCONA COFFEE MIXES LATTE 148G 10PK", "Drinks");
        assertCategory("PAMS VALUE WHITE TOAST BREAD 600G", "Bakery");
        assertCategory("NZ BEEF SCHNITZEL", "Meat");
        assertCategory("WW DRESSING RANCH 300ML", "Condiments & Sauces");
        assertCategory("#SALVATION ARMY DONATION", "Donations");
    }

    @Test
    public void longestProductNameRuleWins() {
        add("rice", "Food", "Pantry");
        add("sushi rice", "Food", "Pantry");

        assertCategory("MAMASAN SUSHI RICE 1KG", "Pantry");
    }

    @Test
    public void packageUnitsDoNotClassifyAnything() {
        assertNull(classifier.classify("500G"));
        assertNull(classifier.classify("2L"));
        assertNull(classifier.classify("10PK"));
        assertNull(classifier.classify("20EA"));
    }

    @Test
    public void unitChangesDoNotChangeProductCategory() {
        assertCategory("WW MILK STANDARD 3L", "Dairy & Eggs");
        assertCategory("WW MILK STANDARD 1L", "Dairy & Eggs");
        assertCategory("WW MILK STANDARD 500ML", "Dairy & Eggs");
    }

    @Test
    public void doesNotMatchKeywordInsideAnotherWord() {
        add("egg", "Food", "Dairy & Eggs");
        assertNull(classifier.classify("Veggie burger"));
    }

    private void assertCategory(String itemName, String expectedSubCategory) {
        Category result = classifier.classify(itemName);
        assertNotNull("Expected category for: " + itemName, result);
        assertEquals(expectedSubCategory, result.getName());
    }

    private void add(String keyword, String parentName, String subCategoryName) {
        Category parent =
                new Category(UUID.randomUUID().toString(), parentName, null);

        Category child =
                new Category(UUID.randomUUID().toString(), subCategoryName, parent);

        repository.addRule(keyword, child);
    }

    private static final class MockCategoryRepository
            implements ICategoryRepository {

        private final Map<String, Category> rules = new HashMap<>();

        void addRule(String keyword, Category category) {
            rules.put(keyword, category);
        }

        @Override
        public Map<String, Category> getAllClassificationRules() {
            return rules;
        }

        @Override
        public Category findCategoryByName(String name, String parentName) {
            return null;
        }

        @Override
        public void saveCategory(Category category) { }

        @Override
        public void saveRule(String keyword, String categoryId) { }
    }
}
