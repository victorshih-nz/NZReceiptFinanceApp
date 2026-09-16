package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.nzreceiptapp.domain.logic.ReceiptValidator;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

public class UpdateReceiptUseCaseTest {

    private UpdateReceiptUseCase useCase;

    @Mock
    private IReceiptRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new UpdateReceiptUseCase(repository);
    }

    @Test
    public void execute_withValidReceipt_updatesSameReceipt() {
        Receipt receipt = validReceipt("receipt-123", "Woolworths", "Albany");

        useCase.execute(receipt);

        verify(repository).updateReceipt(receipt);
        assertEquals("receipt-123", receipt.getId());
    }

    @Test
    public void execute_withBlankBranch_updatesReceipt() {
        Receipt receipt = validReceipt("receipt-123", "Woolworths", "   ");

        useCase.execute(receipt);

        verify(repository).updateReceipt(receipt);
    }

    @Test
    public void execute_withInvalidReceipt_doesNotUpdate() {
        Receipt receipt = validReceipt("receipt-123", " !!! ", null);

        try {
            useCase.execute(receipt);
            fail("Expected ReceiptValidationException");
        } catch (UpdateReceiptUseCase.ReceiptValidationException exception) {
            assertEquals(ReceiptValidator.ErrorCode.CHAIN_REQUIRED,
                    exception.getValidationResult().getErrorCode());
        }

        verify(repository, never()).updateReceipt(any());
    }

    private Receipt validReceipt(String id, String chain, String branch) {
        ReceiptItem item = new ReceiptItem(
                "item-id",
                "Apple",
                "Apple",
                1,
                "EA",
                100,
                Collections.emptyList(),
                null,
                false
        );
        return new Receipt(
                id,
                new Store("store-id", chain, branch),
                Collections.singletonList(item),
                LocalDateTime.of(2026, 8, 17, 12, 0),
                0,
                false
        );
    }
}
