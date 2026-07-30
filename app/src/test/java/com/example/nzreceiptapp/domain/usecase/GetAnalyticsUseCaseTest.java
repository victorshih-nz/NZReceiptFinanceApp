package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.CategorySpending;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetAnalyticsUseCaseTest {

    private GetAnalyticsUseCase useCase;

    @Mock
    private IReceiptRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAnalyticsUseCase(repository);
    }

    @Test
    public void testExecute_CalculatesSpendingCorrectly() {
        // Setup mock categories
        Category dairy = new Category("1", "Dairy", null);
        Category snacks = new Category("2", "Snacks", null);

        // Setup mock items
        ReceiptItem item1 = new ReceiptItem("i1", "Milk", "Milk", 1.0, "ea", 300, Collections.emptyList(), dairy, false);
        ReceiptItem item2 = new ReceiptItem("i2", "Chips", "Chips", 2.0, "ea", 150, Collections.emptyList(), snacks, false);
        ReceiptItem item3 = new ReceiptItem("i3", "Cheese", "Cheese", 1.0, "ea", 500, Collections.emptyList(), dairy, false);

        // Setup mock receipt
        Receipt receipt = new Receipt("r1", null, Arrays.asList(item1, item2, item3), null, 0, false);
        
        when(repository.getAllReceipts()).thenReturn(Collections.singletonList(receipt));

        // Execute
        List<CategorySpending> results = useCase.execute();

        // Verify
        assertEquals(2, results.size());
        
        long dairyTotal = results.stream()
                .filter(s -> s.getCategoryName().equals("Dairy"))
                .findFirst().get().getTotalAmountCents();
        assertEquals(800, dairyTotal);

        long snacksTotal = results.stream()
                .filter(s -> s.getCategoryName().equals("Snacks"))
                .findFirst().get().getTotalAmountCents();
        assertEquals(300, snacksTotal);
    }
}
