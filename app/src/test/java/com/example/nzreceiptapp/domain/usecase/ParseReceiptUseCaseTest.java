package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ParseReceiptUseCaseTest {

    private ParseReceiptUseCase useCase;
    private MockParserFactory mockFactory;

    @Before
    public void setUp() {
        mockFactory = new MockParserFactory();
        useCase = new ParseReceiptUseCase(mockFactory);
    }

    @Test
    public void testExecute_SuccessfulParsing() {
        String rawText = "Mock receipt content";
        String chainName = "Woolworths";
        String branchName = "Albany";
        LocalDateTime date = LocalDateTime.of(2023, 10, 27, 10, 0);

        Receipt receipt = useCase.execute(rawText, chainName, branchName, date);

        assertNotNull(receipt);
        assertEquals(chainName, receipt.getStore().getChainName());
        assertEquals(branchName, receipt.getStore().getBranchName());
        assertEquals(date, receipt.getPurchaseDate());
        assertEquals(1, receipt.getItems().size());
        assertEquals("Mock Item", receipt.getItems().get(0).getName());
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
        assertEquals("Unknown Branch", receipt.getStore().getBranchName());
        assertEquals("content://receipt", receipt.getImageUri());
    }

    private static class MockParserFactory implements IParserFactory {
        @Override
        public IReceiptParser getParser(String chainName) {
            if ("Woolworths".equals(chainName)) {
                return text -> Collections.singletonList(
                    new ReceiptItem("id", "raw", "Mock Item", 1.0, "ea", 100, Collections.emptyList(), null, false)
                );
            }
            return null;
        }

        @Override
        public String detectChain(String rawText) {
            return rawText.contains("WOOLWORTHS") ? "Woolworths" : null;
        }
    }
}
