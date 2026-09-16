package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

public class GetAllItemsPagedUseCaseTest {
    @Mock
    private IReceiptRepository repository;

    private GetAllItemsPagedUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAllItemsPagedUseCase(repository);
    }

    @Test
    public void execute_validRequestReturnsRepositoryPage() {
        PageResult<ReceiptItemSummary> expected = new PageResult<>(
                Collections.emptyList(), 2, 30, 61);
        when(repository.getAllItemsPage(2, 30)).thenReturn(expected);

        PageResult<ReceiptItemSummary> result = useCase.execute(2, 30);

        assertSame(expected, result);
        verify(repository).getAllItemsPage(2, 30);
    }

    @Test
    public void execute_acceptsAllSupportedPageSizes() {
        for (int pageSize : new int[]{15, 30, 50}) {
            PageResult<ReceiptItemSummary> expected = new PageResult<>(
                    Collections.emptyList(), 1, pageSize, 0);
            when(repository.getAllItemsPage(1, pageSize)).thenReturn(expected);

            assertSame(expected, useCase.execute(1, pageSize));
            verify(repository).getAllItemsPage(1, pageSize);
        }
    }

    @Test
    public void execute_rejectsPageBelowOneWithoutCallingRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(0, 30));

        verify(repository, never()).getAllItemsPage(0, 30);
    }

    @Test
    public void execute_rejectsUnsupportedPageSizeWithoutCallingRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(1, 10));

        verify(repository, never()).getAllItemsPage(1, 10);
    }
}
