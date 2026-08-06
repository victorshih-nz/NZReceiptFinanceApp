package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class ReceiptDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadReceipt_exposesReceiptFromUseCase() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        Receipt expected = new Receipt("receipt-1", null, null, null, 0, false);
        when(useCase.execute("receipt-1")).thenReturn(expected);
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, Runnable::run);

        viewModel.loadReceipt("receipt-1");

        assertEquals(expected, viewModel.getReceipt().getValue());
        assertEquals(Boolean.FALSE, viewModel.getIsLoading().getValue());
    }

    @Test
    public void loadReceipt_reportsMissingReceipt() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, Runnable::run);

        viewModel.loadReceipt("missing");

        assertEquals("Receipt not found", viewModel.getErrorMessages().getValue());
    }
}
