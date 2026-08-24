package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    @Mock private DeleteReceiptUseCase deleteUseCase;

    private HistoryViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new HistoryViewModel(
                getReceiptsPagedUseCase,
                deleteUseCase,
                Runnable::run
        );
    }

    @Test
    public void loadData_usesDefaultPageSize30() {
        List<Receipt> expected = Collections.singletonList(
                new Receipt("id", null, null, null, 0, false)
        );
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(expected, 100));

        viewModel.loadData();

        assertEquals(expected, viewModel.getReceipts().getValue());
        assertEquals(Integer.valueOf(30), viewModel.getPageSize().getValue());
        verify(getReceiptsPagedUseCase).executeWithCount(0, 30);
    }

    @Test
    public void setPageSize_changesPageSizeAndReturnsToPage1() {
        List<Receipt> expected = Collections.singletonList(
                new Receipt("id", null, null, null, 0, false)
        );
        when(getReceiptsPagedUseCase.executeWithCount(0, 15)).thenReturn(new PageResult<>(expected, 20));

        viewModel.setPageSize(15);

        assertEquals(Integer.valueOf(15), viewModel.getPageSize().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
        verify(getReceiptsPagedUseCase).executeWithCount(0, 15);
    }

    @Test
    public void nextPage_incrementsPageNumber() {
        List<Receipt> page1Items = new ArrayList<>();
        page1Items.add(new Receipt("id1", null, null, null, 0, false));
        page1Items.add(new Receipt("id2", null, null, null, 0, false));

        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(page1Items, 100));
        when(getReceiptsPagedUseCase.executeWithCount(1, 30)).thenReturn(new PageResult<>(page1Items, 100));

        viewModel.loadData();
        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());

        viewModel.nextPage();
        assertEquals(Integer.valueOf(1), viewModel.getCurrentPage().getValue());
        verify(getReceiptsPagedUseCase).executeWithCount(1, 30);
    }

    @Test
    public void prevPage_decrementsPageNumber() {
        List<Receipt> items = new ArrayList<>();
        items.add(new Receipt("id", null, null, null, 0, false));

        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(items, 100));
        when(getReceiptsPagedUseCase.executeWithCount(1, 30)).thenReturn(new PageResult<>(items, 100));

        viewModel.loadData();
        viewModel.nextPage();
        assertEquals(Integer.valueOf(1), viewModel.getCurrentPage().getValue());

        viewModel.prevPage();
        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void goToPage_navigatesToSpecificPage() {
        List<Receipt> items = new ArrayList<>();
        items.add(new Receipt("id", null, null, null, 0, false));

        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(items, 100));
        when(getReceiptsPagedUseCase.executeWithCount(2, 30)).thenReturn(new PageResult<>(items, 100));

        viewModel.loadData();
        // Set initial state where there are enough pages
        viewModel.goToPage(2);

        assertEquals(Integer.valueOf(2), viewModel.getCurrentPage().getValue());
        verify(getReceiptsPagedUseCase).executeWithCount(2, 30);
    }

    @Test
    public void loadData_calculatesTotalPagesAsLastPageWhenFewerItemsReturned() {
        List<Receipt> fewerItems = new ArrayList<>();
        fewerItems.add(new Receipt("id1", null, null, null, 0, false));
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(fewerItems, 1));

        viewModel.loadData();

        assertEquals(Integer.valueOf(1), viewModel.getTotalPages().getValue());
    }

    @Test
    public void deleteReceipt_deletesThenReloadsCurrentPage() {
        List<Receipt> expected = Collections.singletonList(
                new Receipt("id", null, null, null, 0, false)
        );
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(expected, 1));

        viewModel.deleteReceipt("test-id");

        verify(deleteUseCase).execute("test-id");
        verify(getReceiptsPagedUseCase).executeWithCount(0, 30);
    }
    @Test
    public void nextOnLastPage_doesNothing() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 10)); // 1 page

        viewModel.loadData();
        org.mockito.Mockito.reset(getReceiptsPagedUseCase);

        // already last page
        viewModel.nextPage();

        // still page 0 and no additional loads
        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
        org.mockito.Mockito.verifyNoMoreInteractions(getReceiptsPagedUseCase);
    }

    @Test
    public void prevOnFirstPage_doesNothing() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 10));

        viewModel.loadData();
        org.mockito.Mockito.reset(getReceiptsPagedUseCase);

        viewModel.prevPage();

        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
        org.mockito.Mockito.verifyNoMoreInteractions(getReceiptsPagedUseCase);
    }

    @Test
    public void goToPage_invalidNegative_isRejected() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 100));

        viewModel.loadData();
        org.mockito.Mockito.reset(getReceiptsPagedUseCase);

        viewModel.goToPage(-1);

        // still page 0 and no additional loads
        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
        org.mockito.Mockito.verifyNoMoreInteractions(getReceiptsPagedUseCase);
    }

    @Test
    public void goToPage_invalidTooLarge_isRejected() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 30)); // 1 page

        viewModel.loadData();
        org.mockito.Mockito.reset(getReceiptsPagedUseCase);

        viewModel.goToPage(1); // invalid since pages==1

        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
        org.mockito.Mockito.verifyNoMoreInteractions(getReceiptsPagedUseCase);
    }

    @Test
    public void goToPage_selectCurrent_doesNotReload() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 100));

        viewModel.loadData();

        // reset interactions so we can observe subsequent calls
        org.mockito.Mockito.reset(getReceiptsPagedUseCase);

        viewModel.goToPage(0); // selecting current page

        org.mockito.Mockito.verifyNoMoreInteractions(getReceiptsPagedUseCase);
    }

    @Test
    public void emptyResult_givesOneValidPage() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 0));

        viewModel.loadData();

        assertEquals(Integer.valueOf(1), viewModel.getTotalPages().getValue());
        assertEquals(Integer.valueOf(0), viewModel.getCurrentPage().getValue());
    }

    @Test
    public void exactTotalCalculation_boundary30And31() {
        // 30 items => 1 page with pageSize 30
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.nCopies(30, new Receipt("id", null, null, null, 0, false)), 30));
        viewModel.loadData();
        assertEquals(Integer.valueOf(1), viewModel.getTotalPages().getValue());

        // 31 items => 2 pages
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.nCopies(30, new Receipt("id", null, null, null, 0, false)), 31));
        viewModel.loadData();
        assertEquals(Integer.valueOf(2), viewModel.getTotalPages().getValue());
    }

    @Test
    public void invalidPageSize_isIgnored() {
        when(getReceiptsPagedUseCase.executeWithCount(0, 30)).thenReturn(new PageResult<>(Collections.emptyList(), 0));
        viewModel.loadData();
        org.mockito.Mockito.reset(getReceiptsPagedUseCase);

        // attempt invalid page size
        viewModel.setPageSize(20);

        // value stays default and nothing new loaded
        assertEquals(Integer.valueOf(30), viewModel.getPageSize().getValue());
        org.mockito.Mockito.verifyNoMoreInteractions(getReceiptsPagedUseCase);
    }

}