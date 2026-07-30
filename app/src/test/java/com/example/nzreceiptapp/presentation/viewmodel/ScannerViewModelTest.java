package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
        viewModel = new ScannerViewModel(ocrService, parseUseCase, saveUseCase);
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
        Receipt mockReceipt = new Receipt("id", null, null, null, 0, false);
        when(parseUseCase.execute(anyString(), anyString(), anyString(), any())).thenReturn(mockReceipt);

        // 3. Execute
        viewModel.processReceiptImage("path", "Woolworths", "Albany");

        // 4. Verify
        assertEquals(ScannerViewModel.State.SUCCESS, viewModel.getState().getValue());
        assertEquals(mockReceipt, viewModel.getLastProcessedReceipt().getValue());
        verify(saveUseCase).execute(mockReceipt);
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
        assertEquals(ScannerViewModel.State.ERROR, viewModel.getState().getValue());
        assertEquals("OCR failed: OCR Error", viewModel.getErrorMessages().getValue());
    }
}
