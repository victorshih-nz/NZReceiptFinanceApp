package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;
import com.example.nzreceiptapp.domain.service.ICategoryInitializer;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ParseReceiptUseCaseTest {

    private ParseReceiptUseCase useCase;
    private RecordingCategoryInitializer categoryInitializer;

    @Before
    public void setUp() {
        MockCategoryRepository categoryRepository = new MockCategoryRepository();
        categoryRepository.addRule(
                "mock item", new Category("category", "Test Category", null));
        categoryInitializer = new RecordingCategoryInitializer();
        useCase = new ParseReceiptUseCase(
                new MockParserFactory(),
                categoryInitializer,
                new CategoryClassifier(categoryRepository));
    }

    @Test
    public void testExecute_SuccessfulParsingAndClassification() {
        String rawText = "Mock receipt content";
        String chainName = "Woolworths";
        String branchName = "Albany";
        LocalDateTime date = LocalDateTime.of(2023, 10, 27, 10, 0);

        Receipt receipt = useCase.execute(rawText, chainName, branchName, date);

        assertNotNull(receipt);
        assertTrue(categoryInitializer.wasInitialized);
        assertEquals(chainName, receipt.getStore().getChainName());
        assertEquals(branchName, receipt.getStore().getBranchName());
        assertEquals(date, receipt.getPurchaseDate());
        assertEquals(1, receipt.getItems().size());
        assertEquals("Mock Item", receipt.getItems().get(0).getName());
        assertNotNull(receipt.getItems().get(0).getCategory());
        assertEquals("Test Category", receipt.getItems().get(0).getCategory().getName());
        assertEquals(Long.valueOf(100), receipt.getPrintedTotalCents());
        assertEquals(rawText, receipt.getRawOcrText());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExecute_UnsupportedChain() {
        useCase.execute("text", "UnknownStore", "Branch", null);
    }

    @Test
    public void testExecute_AutoDetectsChain() {
        Receipt receipt = useCase.execute(
                "WOOLWORTHS\nMock receipt content",
                "Auto detect",
                "",
                null,
                "content://receipt");

        assertEquals("Woolworths", receipt.getStore().getChainName());
        assertEquals("", receipt.getStore().getBranchName());
        assertEquals("content://receipt", receipt.getImageUri());
    }

    @Test
    public void testExecute_WhitespaceBranchBecomesEmpty() {
        Receipt receipt = useCase.execute(
                "Mock receipt content",
                "Woolworths",
                "   ",
                LocalDateTime.of(2026, 8, 17, 12, 0));

        assertEquals("", receipt.getStore().getBranchName());
    }

    private static class MockParserFactory implements IParserFactory {
        @Override
        public IReceiptParser getParser(String chainName) {
            if ("Woolworths".equals(chainName)) {
                return text -> new ParsedReceipt(
                        Collections.singletonList(new ReceiptItem(
                                "id", "raw", "Mock Item", 1.0, "ea", 100,
                                Collections.emptyList(), null, false)),
                        100L);
            }
            return null;
        }

        @Override
        public String detectChain(String rawText) {
            return rawText.contains("WOOLWORTHS") ? "Woolworths" : null;
        }
    }

    private static class RecordingCategoryInitializer implements ICategoryInitializer {
        private boolean wasInitialized;

        @Override
        public void ensureInitialized() {
            wasInitialized = true;
        }
    }

    private static class MockCategoryRepository implements ICategoryRepository {
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
        public void saveCategory(Category category) {
        }

        @Override
        public void saveRule(String keyword, String categoryId) {
        }
    }
}
