package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.AnalyticsPeriod;
import com.example.nzreceiptapp.domain.model.AnalyticsSummary;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.CategorySpending;
import com.example.nzreceiptapp.domain.model.ItemDiscount;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GetAnalyticsUseCaseTest {
    @Mock private IReceiptRepository repository;
    private GetAnalyticsUseCase useCase;
    private final Category food = new Category("parent", "Food", null);
    private final Category dairy = new Category("dairy", "Dairy", food);
    private final Category snacks = new Category("snacks", "Snacks", food);

    @Before public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAnalyticsUseCase(repository);
    }

    private ReceiptItem item(String id, long priceCents, Category category, long itemDiscount) {
        List<ItemDiscount> discounts = itemDiscount == 0 ? Collections.emptyList() :
                Collections.singletonList(new ItemDiscount(ItemDiscount.DiscountType.MEMBER_SAVING,
                        "saving", itemDiscount));
        return new ReceiptItem(id, id, id, 1.0, "EA", priceCents, discounts, category, false);
    }

    private Receipt receipt(String id, LocalDateTime date, long receiptDiscount, ReceiptItem... items) {
        return new Receipt(id, null, Arrays.asList(items), date, receiptDiscount, false);
    }

    @Test public void calculatesPaidAndCategoryTotalsWithoutAllocatingReceiptDiscount() {
        Receipt jan = receipt("r1", LocalDateTime.of(2026, 1, 5, 13, 0), 300,
                item("milk", 2000, dairy, 100), item("meat", 1000, snacks, 0));
        when(repository.getAllReceipts()).thenReturn(Collections.singletonList(jan));
        AnalyticsSummary summary = useCase.execute(AnalyticsPeriod.month(YearMonth.of(2026, 1)));
        assertEquals(2600, summary.getTotalSpendingCents()); // 2000 -100 +1000 -300
        assertEquals(2900, summary.getCategoryTotalCents());
        assertEquals(300, summary.getCategoryMinusPaidCents());
        assertEquals(1, summary.getReceiptCount());
        assertEquals(2, summary.getItemCount());
        assertEquals("Food / Dairy", summary.getCategories().get(0).getCategoryName());
        assertEquals(1900, summary.getCategories().get(0).getTotalAmountCents());
        assertEquals(1000, summary.getCategories().get(1).getTotalAmountCents());
    }

    @Test public void dayAndMonthBoundariesAreStartInclusiveAndEndExclusive() {
        when(repository.getAllReceipts()).thenReturn(Arrays.asList(
                receipt("prev", LocalDateTime.of(2025,12,31,23,59),0,item("a",100,dairy,0)),
                receipt("start", LocalDateTime.of(2026,1,1,0,0),0,item("b",200,dairy,0)),
                receipt("last", LocalDateTime.of(2026,1,31,23,59,59),0,item("c",300,dairy,0)),
                receipt("next", LocalDateTime.of(2026,2,1,0,0),0,item("d",400,dairy,0)),
                receipt("undated", null,0,item("e",500,dairy,0))));
        assertEquals(500, useCase.execute(AnalyticsPeriod.month(YearMonth.of(2026,1))).getTotalSpendingCents());
        assertEquals(200, useCase.execute(AnalyticsPeriod.day(LocalDate.of(2026,1,1))).getTotalSpendingCents());
        assertEquals(900, useCase.execute(AnalyticsPeriod.year(2026)).getTotalSpendingCents());
    }

    @Test public void separateCategoryIdsAndVisibleUncategorized() {
        Category otherDairy = new Category("different", "Dairy", new Category("other", "Other", null));
        when(repository.getAllReceipts()).thenReturn(Collections.singletonList(receipt("r", LocalDateTime.of(2026,1,1,0,0),0,
                item("a",300,dairy,0),item("b",200,otherDairy,0),item("c",100,null,0))));
        AnalyticsSummary result = useCase.execute(AnalyticsPeriod.year(2026));
        assertEquals(3,result.getCategories().size());
        assertEquals("Food / Dairy",result.getCategories().get(0).getCategoryName());
        assertEquals("Other / Dairy",result.getCategories().get(1).getCategoryName());
        assertEquals("Uncategorized",result.getCategories().get(2).getCategoryName());
        assertNull(result.getCategories().get(2).getCategoryId());
        assertEquals(600,result.getCategoryTotalCents());
    }

    @Test public void categoryOrderingIsDeterministicOnEqualAmounts() {
        when(repository.getAllReceipts()).thenReturn(Collections.singletonList(receipt("r", LocalDateTime.of(2026,1,1,0,0),0,
                item("s",100,snacks,0),item("d",100,dairy,0))));
        List<CategorySpending> categories = useCase.execute(AnalyticsPeriod.year(2026)).getCategories();
        assertEquals("Food / Dairy",categories.get(0).getCategoryName());
        assertEquals("Food / Snacks",categories.get(1).getCategoryName());
    }

    @Test public void zeroFilledMonthAndLeapYearDailySeries() {
        when(repository.getAllReceipts()).thenReturn(Arrays.asList(
                receipt("r1", LocalDateTime.of(2024,2,29,11,0),0,item("a",125,dairy,0)),
                receipt("r2", LocalDateTime.of(2025,2,28,11,0),0,item("b",200,dairy,0)),
                receipt("r3", null,0,item("c",300,dairy,0))));
        Map<Integer,Long> monthly = useCase.getMonthlyTotals(2024);
        assertEquals(12,monthly.size());
        assertEquals(Long.valueOf(125),monthly.get(2));
        assertEquals(Long.valueOf(0),monthly.get(1));
        Map<Integer,Long> daily = useCase.getDailyTotals(YearMonth.of(2024,2));
        assertEquals(29,daily.size());
        assertEquals(Long.valueOf(125),daily.get(29));
        assertEquals(28,useCase.getDailyTotals(YearMonth.of(2025,2)).size());
        assertEquals(Long.valueOf(125),useCase.getYearlyTotals().get(2024));
        assertEquals(Long.valueOf(200),useCase.getYearlyTotals().get(2025));
        assertEquals(2,useCase.getYearlyTotals().size());
    }

    @Test public void emptyPeriodAndLegacyAggregation() {
        when(repository.getAllReceipts()).thenReturn(Collections.singletonList(receipt("r", null,0,item("a",75,dairy,0))));
        AnalyticsSummary empty = useCase.execute(AnalyticsPeriod.year(2026));
        assertTrue(empty.isEmpty());
        assertEquals(0,empty.getTotalSpendingCents());
        assertTrue(empty.getCategories().isEmpty());
        assertEquals(75,useCase.execute().get(0).getTotalAmountCents());
        try { empty.getCategories().add(new CategorySpending("Bad",1)); fail("Must be immutable"); }
        catch (UnsupportedOperationException expected) { /* expected */ }
    }
}
