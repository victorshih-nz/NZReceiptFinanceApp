package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.PageResult;
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
    public InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

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
    public void loadData_loadsOneBasedFirstReceiptPageWithDefaultSize() {
        List<Receipt> expected = Collections.singletonList(receipt("receipt-1"));
        when(getReceiptsPagedUseCase.execute(1, 15))
                .thenReturn(new PageResult<>(expected, 1, 15, 16));

        viewModel.loadData();

        assertEquals(expected, viewModel.getReceipts().getValue());
        assertPaging(1, 15, 2, false, true);
        verify(getReceiptsPagedUseCase).execute(1, 15);
    }

    @Test
    public void nextAndPreviousPage_respectPageResultBoundaries() {
        when(getReceiptsPagedUseCase.execute(1, 15))
                .thenReturn(receiptPage("receipt-1", 1, 16));
        when(getReceiptsPagedUseCase.execute(2, 15))
                .thenReturn(receiptPage("receipt-2", 2, 16));

        viewModel.loadData();
        viewModel.nextPage();

        assertEquals("receipt-2",
                viewModel.getReceipts().getValue().get(0).getId());
        assertPaging(2, 15, 2, true, false);

        viewModel.nextPage();
        verify(getReceiptsPagedUseCase, times(1)).execute(2, 15);

        viewModel.prevPage();
        assertPaging(1, 15, 2, false, true);
        verify(getReceiptsPagedUseCase, times(2)).execute(1, 15);
    }

    @Test
    public void goToPage_loadsValidPageAndIgnoresInvalidPage() {
        when(getReceiptsPagedUseCase.execute(1, 15))
                .thenReturn(receiptPage("receipt-1", 1, 31));
        when(getReceiptsPagedUseCase.execute(3, 15))
                .thenReturn(receiptPage("receipt-31", 3, 31));

        viewModel.loadData();
        viewModel.goToPage(3);
        viewModel.goToPage(4);

        assertPaging(3, 15, 3, true, false);
        verify(getReceiptsPagedUseCase).execute(3, 15);
        verify(getReceiptsPagedUseCase, never()).execute(4, 15);
    }

    @Test
    public void loadData_zeroRecordsUsesPageOneOfOne() {
        when(getReceiptsPagedUseCase.execute(1, 15)).thenReturn(
                new PageResult<>(Collections.emptyList(), 1, 15, 0));

        viewModel.loadData();

        assertEquals(Collections.emptyList(), viewModel.getReceipts().getValue());
        assertPaging(1, 15, 1, false, false);
    }

    @Test
    public void modes_retainIndependentPageAndDefaultPageSize() {
        when(getReceiptsPagedUseCase.execute(1, 15))
                .thenReturn(receiptPage("receipt-1", 1, 16));
        when(getReceiptsPagedUseCase.execute(2, 15))
                .thenReturn(receiptPage("receipt-2", 2, 16));
        when(getAllItemsPagedUseCase.execute(1, 30)).thenReturn(
                new PageResult<>(Collections.emptyList(), 1, 30, 31));

        viewModel.loadData();
        viewModel.nextPage();
        viewModel.setViewMode(HistoryViewModel.ViewMode.ALL_ITEMS);

        assertPaging(1, 30, 2, false, true);
        verify(getAllItemsPagedUseCase).execute(1, 30);

        viewModel.setViewMode(HistoryViewModel.ViewMode.RECEIPTS);

        assertPaging(2, 15, 2, true, false);
        verify(getReceiptsPagedUseCase, times(2)).execute(2, 15);
    }

    @Test
    public void refresh_missingCurrentPageUsesRepositoryEffectivePage() {
        when(getReceiptsPagedUseCase.execute(1, 15))
                .thenReturn(receiptPage("receipt-1", 1, 31));
        when(getReceiptsPagedUseCase.execute(3, 15))
                .thenReturn(
                        receiptPage("receipt-31", 3, 31),
                        receiptPage("receipt-16", 2, 30));

        viewModel.loadData();
        viewModel.goToPage(3);
        viewModel.loadData();

        assertEquals("receipt-16",
                viewModel.getReceipts().getValue().get(0).getId());
        assertPaging(2, 15, 2, true, false);
    }

    @Test
    public void deleteReceipt_deletesThenReloadsRetainedPage() {
        when(getReceiptsPagedUseCase.execute(1, 15))
                .thenReturn(receiptPage("receipt-1", 1, 1));
        viewModel.loadData();

        viewModel.deleteReceipt("receipt-1");

        verify(deleteUseCase).execute("receipt-1");
        verify(getReceiptsPagedUseCase, times(2)).execute(1, 15);
    }

    private void assertPaging(int currentPage,
                              int pageSize,
                              int totalPages,
                              boolean hasPrevious,
                              boolean hasNext) {
        HistoryViewModel.PagingUiState state =
                viewModel.getPagingState().getValue();
        assertEquals(currentPage, state.getCurrentPage());
        assertEquals(pageSize, state.getPageSize());
        assertEquals(totalPages, state.getTotalPages());
        assertEquals(hasPrevious, state.hasPrevious());
        assertEquals(hasNext, state.hasNext());
    }

    private PageResult<Receipt> receiptPage(String id,
                                            int currentPage,
                                            int totalRecords) {
        return new PageResult<>(
                Collections.singletonList(receipt(id)),
                currentPage,
                15,
                totalRecords);
    }

    private Receipt receipt(String id) {
        return new Receipt(id, null, null, null, 0, false);
    }
}
