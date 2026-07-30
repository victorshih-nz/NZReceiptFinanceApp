package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

public class HistoryViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private GetReceiptsUseCase getReceiptsUseCase;
    @Mock
    private DeleteReceiptUseCase deleteUseCase;

    private HistoryViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new HistoryViewModel(getReceiptsUseCase, deleteUseCase);
    }

    @Test
    public void testLoadReceipts() throws InterruptedException {
        // Mock
        List<Receipt> mockList = Collections.singletonList(new Receipt("id", null, null, null, 0, false));
        when(getReceiptsUseCase.execute()).thenReturn(mockList);

        // Execute
        viewModel.loadReceipts();

        // Wait a bit for the thread
        Thread.sleep(100);

        // Verify
        assertEquals(mockList, viewModel.getReceipts().getValue());
    }

    @Test
    public void testDeleteReceipt() throws InterruptedException {
        // Execute
        viewModel.deleteReceipt("test-id");

        // Wait a bit
        Thread.sleep(100);

        // Verify
        verify(deleteUseCase).execute("test-id");
        verify(getReceiptsUseCase).execute(); // Should reload
    }
}
