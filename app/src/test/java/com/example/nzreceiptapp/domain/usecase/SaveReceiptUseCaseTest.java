package com.example.nzreceiptapp.domain.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        Receipt receipt = new Receipt("id", null, null, null, 0, false);

        useCase.execute(receipt);

        verify(repository).saveReceipt(receipt);
    }

    @Test
    public void execute_withNull_doesNotSave() {
        useCase.execute(null);

        verify(repository, never()).saveReceipt(any());
    }
}
