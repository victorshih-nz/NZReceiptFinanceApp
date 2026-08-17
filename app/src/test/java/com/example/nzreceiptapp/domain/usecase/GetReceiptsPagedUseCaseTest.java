package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

public class GetReceiptsPagedUseCaseTest {
    @Mock
    private IReceiptRepository repository;

    private GetReceiptsPagedUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetReceiptsPagedUseCase(repository);
    }

    @Test
    public void execute_validRequestReturnsRepositoryPage() {
        PageResult<Receipt> expected = new PageResult<>(
                Collections.emptyList(), 2, 15, 31);
        when(repository.getReceiptsPage(2, 15)).thenReturn(expected);

        PageResult<Receipt> result = useCase.execute(2, 15);

        assertSame(expected, result);
        verify(repository).getReceiptsPage(2, 15);
    }

    @Test
    public void execute_acceptsAllSupportedPageSizes() {
        for (int pageSize : new int[]{15, 30, 50}) {
            PageResult<Receipt> expected = new PageResult<>(
                    Collections.emptyList(), 1, pageSize, 0);
            when(repository.getReceiptsPage(1, pageSize)).thenReturn(expected);

            assertSame(expected, useCase.execute(1, pageSize));
            verify(repository).getReceiptsPage(1, pageSize);
        }
    }

    @Test
    public void execute_rejectsPageBelowOneWithoutCallingRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(0, 15));

        verify(repository, never()).getReceiptsPage(0, 15);
    }

    @Test
    public void execute_rejectsUnsupportedPageSizeWithoutCallingRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(1, 10));

        verify(repository, never()).getReceiptsPage(1, 10);
    }
}
