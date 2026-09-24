package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.AnalyticsPeriod;
import com.example.nzreceiptapp.domain.model.AnalyticsSummary;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.CategorySpending;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Domain-only analytics. Uses stored purchase dates and integer cents. */
public class GetAnalyticsUseCase {
    private static final String UNCATEGORIZED_KEY = "__uncategorized__";
    private static final String UNCATEGORIZED_NAME = "Uncategorized";
    private final IReceiptRepository repository;

    public GetAnalyticsUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    /** Legacy API: category totals across all receipts (including undated receipts). */
    public List<CategorySpending> execute() {
        return calculate(repository.getAllReceipts(), null).getCategories();
    }

    /** Period calculation; undated receipts are excluded because the period is unknown. */
    public AnalyticsSummary execute(AnalyticsPeriod period) {
        if (period == null) throw new IllegalArgumentException("period is required");
        return calculate(repository.getReceiptsBetween(
                period.getStartInclusive(), period.getEndExclusive()), period);
    }

    /** Ascending year -> paid cents. Null purchase dates are omitted. */
    public Map<Integer, Long> getYearlyTotals() {
        Map<Integer, Long> totals = new TreeMap<>();
        for (Receipt receipt : repository.getDatedReceipts()) {
            if (receipt.getPurchaseDate() != null) {
                int year = receipt.getPurchaseDate().getYear();
                totals.merge(year, receipt.getFinalPayableCents(), Long::sum);
            }
        }
        return Collections.unmodifiableMap(totals);
    }

    /** Month 1..12 -> paid cents, including zero-spend months. */
    public Map<Integer, Long> getMonthlyTotals(int year) {
        Map<Integer, Long> totals = new TreeMap<>();
        for (int month = 1; month <= 12; month++) totals.put(month, 0L);
        AnalyticsPeriod period = AnalyticsPeriod.year(year);
        for (Receipt receipt : repository.getReceiptsBetween(
                period.getStartInclusive(), period.getEndExclusive())) {
            LocalDateTime date = receipt.getPurchaseDate();
            if (period.contains(date)) {
                totals.merge(date.getMonthValue(), receipt.getFinalPayableCents(), Long::sum);
            }
        }
        return Collections.unmodifiableMap(totals);
    }

    /** Day 1..lengthOfMonth -> paid cents, including zero-spend days. */
    public Map<Integer, Long> getDailyTotals(YearMonth month) {
        if (month == null) throw new IllegalArgumentException("month is required");
        Map<Integer, Long> totals = new TreeMap<>();
        for (int day = 1; day <= month.lengthOfMonth(); day++) totals.put(day, 0L);
        AnalyticsPeriod period = AnalyticsPeriod.month(month);
        for (Receipt receipt : repository.getReceiptsBetween(
                period.getStartInclusive(), period.getEndExclusive())) {
            LocalDateTime date = receipt.getPurchaseDate();
            if (period.contains(date)) {
                totals.merge(date.getDayOfMonth(), receipt.getFinalPayableCents(), Long::sum);
            }
        }
        return Collections.unmodifiableMap(totals);
    }

    private AnalyticsSummary calculate(List<Receipt> receipts, AnalyticsPeriod period) {
        Map<String, Long> amounts = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        long paid = 0;
        long itemTotal = 0;
        int receiptCount = 0;
        int itemCount = 0;
        for (Receipt receipt : receipts) {
            if (period != null && !period.contains(receipt.getPurchaseDate())) continue;
            receiptCount++;
            paid = Math.addExact(paid, receipt.getFinalPayableCents());
            if (receipt.getItems() == null) continue;
            for (ReceiptItem item : receipt.getItems()) {
                itemCount++;
                long amount = item.getFinalSubtotalCents();
                itemTotal = Math.addExact(itemTotal, amount);
                Category category = item.getCategory();
                String id = category == null ? UNCATEGORIZED_KEY : category.getId();
                if (id == null || id.trim().isEmpty()) id = UNCATEGORIZED_KEY;
                String label = category == null || UNCATEGORIZED_KEY.equals(id)
                        ? UNCATEGORIZED_NAME : categoryLabel(category);
                amounts.merge(id, amount, Math::addExact);
                labels.put(id, label);
            }
        }
        List<CategorySpending> categories = new ArrayList<>();
        for (Map.Entry<String, Long> entry : amounts.entrySet()) {
            String id = entry.getKey();
            categories.add(new CategorySpending(UNCATEGORIZED_KEY.equals(id) ? null : id,
                    labels.get(id), entry.getValue()));
        }
        categories.sort(Comparator.comparingLong(CategorySpending::getTotalAmountCents).reversed()
                .thenComparing(CategorySpending::getCategoryName)
                .thenComparing(c -> c.getCategoryId() == null ? "" : c.getCategoryId()));
        return new AnalyticsSummary(paid, itemTotal, receiptCount, itemCount, categories);
    }

    /** Leaf categories stay separate; the parent prefix disambiguates repeated names. */
    private static String categoryLabel(Category category) {
        Category parent = category.getParentCategory();
        return parent == null ? category.getName() : parent.getName() + " / " + category.getName();
    }
}
