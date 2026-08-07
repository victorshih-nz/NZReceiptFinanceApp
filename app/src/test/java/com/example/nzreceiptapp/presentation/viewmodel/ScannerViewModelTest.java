package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

public class ScannerViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private IOCRService ocrService;
    @Mock
    private ParseReceiptUseCase parseUseCase;
    @Mock
    private SaveReceiptUseCase saveUseCase;

    private ScannerViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new ScannerViewModel(ocrService, parseUseCase, saveUseCase, Runnable::run);
    }

    @Test
    public void testProcessReceiptImage_Success() {
        // 1. Mock OCR Success
        doAnswer(invocation -> {
            IOCRService.OnOCRCompleteListener listener = invocation.getArgument(1);
            listener.onSuccess("Mock Text");
            return null;
        }).when(ocrService).extractText(anyString(), any());

        // 2. Mock Parsing Success
        ReceiptItem item = new ReceiptItem(
                "item", "raw", "Milk", 1, "ea", 399,
                Collections.emptyList(), null, false);
        Receipt mockReceipt = new Receipt(
                "id", new Store("store", "Woolworths", "Albany"),
                Collections.singletonList(item), null, 0, false,
                "Mock Text", "path", 399L);
        when(parseUseCase.execute(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(mockReceipt);

        // 3. Execute
        viewModel.processReceiptImage("path", "Woolworths", "Albany");

        // 4. Verify
        assertEquals(ScannerUiState.Phase.READY_FOR_REVIEW,
                viewModel.getUiState().getValue().getPhase());
        assertEquals(mockReceipt, viewModel.getUiState().getValue().getDraft());
        verify(saveUseCase, never()).execute(any());

        viewModel.saveReviewedReceipt(
                "Woolworths", "Albany", mockReceipt.getItems());

        assertEquals(ScannerUiState.Phase.SAVED,
                viewModel.getUiState().getValue().getPhase());
        verify(saveUseCase).execute(any(Receipt.class));
    }

    @Test
    public void testProcessReceiptImage_OCRFailure() {
        // 1. Mock OCR Failure
        doAnswer(invocation -> {
            IOCRService.OnOCRCompleteListener listener = invocation.getArgument(1);
            listener.onFailure(new Exception("OCR Error"));
            return null;
        }).when(ocrService).extractText(anyString(), any());

        // 2. Execute
        viewModel.processReceiptImage("path", "Woolworths", "Albany");

        // 3. Verify
        assertEquals(ScannerUiState.Phase.ERROR,
                viewModel.getUiState().getValue().getPhase());
        assertEquals("OCR failed: OCR Error",
                viewModel.getUiState().getValue().getErrorMessage());
    }
}
