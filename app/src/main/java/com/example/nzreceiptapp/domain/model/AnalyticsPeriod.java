package com.example.nzreceiptapp.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

/** An inclusive-start, exclusive-end period based on the saved purchase date. */
public final class AnalyticsPeriod {
    private final LocalDateTime startInclusive;
    private final LocalDateTime endExclusive;

    private AnalyticsPeriod(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (!startInclusive.isBefore(endExclusive)) {
            throw new IllegalArgumentException("Analytics period must have positive duration");
        }
        this.startInclusive = startInclusive;
        this.endExclusive = endExclusive;
    }

    public static AnalyticsPeriod year(int year) {
        return between(LocalDate.of(year, 1, 1), LocalDate.of(year + 1, 1, 1));
    }

    public static AnalyticsPeriod month(YearMonth month) {
        Objects.requireNonNull(month, "month");
        return between(month.atDay(1), month.plusMonths(1).atDay(1));
    }

    public static AnalyticsPeriod day(LocalDate day) {
        Objects.requireNonNull(day, "day");
        return between(day, day.plusDays(1));
    }

    public static AnalyticsPeriod between(LocalDate startInclusive, LocalDate endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        return new AnalyticsPeriod(startInclusive.atStartOfDay(), endExclusive.atStartOfDay());
    }

    public boolean contains(LocalDateTime purchaseDate) {
        return purchaseDate != null && !purchaseDate.isBefore(startInclusive)
                && purchaseDate.isBefore(endExclusive);
    }

    public LocalDateTime getStartInclusive() { return startInclusive; }
    public LocalDateTime getEndExclusive() { return endExclusive; }
}
