package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertTrue;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class SaveReceiptUseCaseTest {

    private SaveReceiptUseCase useCase;
    private MockReceiptRepository mockRepository;

    @Before
    public void setUp() {
        mockRepository = new MockReceiptRepository();
        useCase = new SaveReceiptUseCase(mockRepository);
    }

    @Test
    public void testExecute_CallsRepository() {
        useCase.execute(null); // Should handle null
        
        Receipt mockReceipt = new Receipt("id", null, null, null, 0, false);
        useCase.execute(mockReceipt);

        assertTrue(mockRepository.saveCalled);
    }

    private static class MockReceiptRepository implements IReceiptRepository {
        boolean saveCalled = false;

        @Override
        public void saveReceipt(Receipt receipt) {
            saveCalled = true;
        }

        @Override
        public List<Receipt> getAllReceipts() { return null; }

        @Override
        public void deleteReceipt(String id) {}
    }
}
