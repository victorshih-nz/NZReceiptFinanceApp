package com.example.nzreceiptapp.domain.usecase;

import static org.mockito.Mockito.verify;

import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DeleteReceiptUseCaseTest {

    @Mock
    private IReceiptRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void execute_deletesReceiptById() {
        DeleteReceiptUseCase useCase = new DeleteReceiptUseCase(repository);

        useCase.execute("test-id");

        verify(repository).deleteReceipt("test-id");
    }
}
