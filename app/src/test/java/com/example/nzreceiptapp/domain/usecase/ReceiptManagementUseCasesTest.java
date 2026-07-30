package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

public class ReceiptManagementUseCasesTest {

    @Mock
    private IReceiptRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetReceiptsUseCase() {
        GetReceiptsUseCase useCase = new GetReceiptsUseCase(repository);
        List<Receipt> mockList = Collections.singletonList(new Receipt("id", null, null, null, 0, false));
        when(repository.getAllReceipts()).thenReturn(mockList);

        List<Receipt> result = useCase.execute();

        assertEquals(1, result.size());
        assertEquals("id", result.get(0).getId());
    }

    @Test
    public void testDeleteReceiptUseCase() {
        DeleteReceiptUseCase useCase = new DeleteReceiptUseCase(repository);
        String id = "test-id";

        useCase.execute(id);

        verify(repository).deleteReceipt(id);
    }
}
