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

public class SaveReceiptUseCaseTest {

    private SaveReceiptUseCase useCase;

    @Mock
    private IReceiptRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new SaveReceiptUseCase(repository);
    }

    @Test
    public void execute_withReceipt_savesReceipt() {
        Receipt receipt = validReceipt("Woolworths", "Greenlane");

        useCase.execute(receipt);

        verify(repository).saveReceipt(receipt);
    }

    @Test
    public void execute_withBlankBranch_savesReceipt() {
        Receipt receipt = validReceipt("Woolworths", "   ");

        useCase.execute(receipt);

        verify(repository).saveReceipt(receipt);
    }

    @Test
    public void execute_withNull_throwsValidationErrorAndDoesNotSave() {
        assertValidationError(null, ReceiptValidator.ErrorCode.RECEIPT_REQUIRED);

        verify(repository, never()).saveReceipt(any());
    }

    @Test
    public void execute_withNormalizedEmptyChain_throwsValidationErrorAndDoesNotSave() {
        Receipt receipt = validReceipt(" !!! ", null);

        assertValidationError(receipt, ReceiptValidator.ErrorCode.CHAIN_REQUIRED);

        verify(repository, never()).saveReceipt(any());
    }

    private void assertValidationError(Receipt receipt,
                                       ReceiptValidator.ErrorCode expectedCode) {
        try {
            useCase.execute(receipt);
            fail("Expected ReceiptValidationException");
        } catch (SaveReceiptUseCase.ReceiptValidationException exception) {
            assertEquals(expectedCode,
                    exception.getValidationResult().getErrorCode());
        }
    }

    private Receipt validReceipt(String chain, String branch) {
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
                "receipt-id",
                new Store("store-id", chain, branch),
                Collections.singletonList(item),
                LocalDateTime.of(2026, 8, 17, 12, 0),
                0,
                false
        );
    }
}
