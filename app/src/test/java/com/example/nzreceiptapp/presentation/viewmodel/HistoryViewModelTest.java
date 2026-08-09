package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;

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

    @Mock private GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    @Mock private GetAllItemsPagedUseCase getAllItemsPagedUseCase;
    @Mock private DeleteReceiptUseCase deleteUseCase;

    private HistoryViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new HistoryViewModel(
                getReceiptsPagedUseCase,
                getAllItemsPagedUseCase,
                deleteUseCase,
                Runnable::run
        );
    }

    @Test
    public void loadData_loadsFirstReceiptPage() {
        List<Receipt> expected = Collections.singletonList(
                new Receipt("id", null, null, null, 0, false)
        );
        when(getReceiptsPagedUseCase.execute(0, 10)).thenReturn(expected);

        viewModel.loadData();

        assertEquals(expected, viewModel.getReceipts().getValue());
        verify(getReceiptsPagedUseCase).execute(0, 10);
    }

    @Test
    public void deleteReceipt_deletesThenReloadsCurrentPage() {
        viewModel.deleteReceipt("test-id");

        verify(deleteUseCase).execute("test-id");
        verify(getReceiptsPagedUseCase).execute(0, 10);
    }
}
