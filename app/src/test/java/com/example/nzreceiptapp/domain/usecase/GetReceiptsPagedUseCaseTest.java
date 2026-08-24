package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import java.util.Collections;
import java.util.List;

public class GetReceiptsPagedUseCaseTest {
    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

    @Mock private IReceiptRepository repository;

    private GetReceiptsPagedUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetReceiptsPagedUseCase(repository);
    }

    @Test
    public void executeWithCount_returnsItemsAndCount() {
        List<Receipt> items = Collections.singletonList(new Receipt("id", null, null, null, 0, false));
        when(repository.getReceiptsPaged(30, 0)).thenReturn(items);
        when(repository.getReceiptsCount()).thenReturn(123);

        com.example.nzreceiptapp.domain.model.PageResult<Receipt> res = useCase.executeWithCount(0, 30);

        assertEquals(123, res.totalCount);
        assertEquals(items, res.items);
    }
}
