package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

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

        ReceiptDetailUiState state = viewModel.getUiState().getValue();
        assertEquals(ReceiptDetailUiState.LoadState.CONTENT, state.getLoadState());
        assertEquals(expected, state.getReceipt());
    }

    @Test
    public void loadReceipt_reportsMissingReceipt() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, Runnable::run);

        viewModel.loadReceipt("missing");

        ReceiptDetailUiState state = viewModel.getUiState().getValue();
        assertEquals(ReceiptDetailUiState.LoadState.ERROR, state.getLoadState());
        assertEquals("Receipt not found", state.getErrorMessage());
    }

    @Test
    public void loadReceipt_exposesLoadingBeforeExecutorCompletes() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        ControlledExecutor executor = new ControlledExecutor();
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, executor);

        viewModel.loadReceipt("receipt-1");

        assertEquals(ReceiptDetailUiState.LoadState.LOADING,
                viewModel.getUiState().getValue().getLoadState());
        executor.runNext();
        assertEquals(ReceiptDetailUiState.LoadState.ERROR,
                viewModel.getUiState().getValue().getLoadState());
    }

    @Test
    public void loadReceipt_missingIdShowsInlineErrorWithoutUseCaseCall() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, Runnable::run);

        viewModel.loadReceipt("  ");

        ReceiptDetailUiState state = viewModel.getUiState().getValue();
        assertEquals(ReceiptDetailUiState.LoadState.ERROR, state.getLoadState());
        assertEquals("Receipt ID is missing", state.getErrorMessage());
        verify(useCase, never()).execute(Mockito.anyString());
    }

    @Test
    public void retry_repeatsFailedLoadForSameReceiptId() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        Receipt expected = new Receipt("receipt-1", null, null, null, 0, false);
        when(useCase.execute("receipt-1"))
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(expected);
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, Runnable::run);

        viewModel.loadReceipt("receipt-1");
        viewModel.retry();

        assertEquals(ReceiptDetailUiState.LoadState.CONTENT,
                viewModel.getUiState().getValue().getLoadState());
        assertEquals(expected, viewModel.getUiState().getValue().getReceipt());
        verify(useCase, times(2)).execute("receipt-1");
    }

    @Test
    public void loadReceipt_doesNotReloadRetainedContent() {
        GetReceiptByIdUseCase useCase = Mockito.mock(GetReceiptByIdUseCase.class);
        Receipt expected = new Receipt("receipt-1", null, null, null, 0, false);
        when(useCase.execute("receipt-1")).thenReturn(expected);
        ReceiptDetailViewModel viewModel = new ReceiptDetailViewModel(useCase, Runnable::run);

        viewModel.loadReceipt("receipt-1");
        viewModel.loadReceipt("receipt-1");

        verify(useCase, times(1)).execute("receipt-1");
    }

    private static final class ControlledExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runNext() {
            tasks.remove().run();
        }
    }
}
