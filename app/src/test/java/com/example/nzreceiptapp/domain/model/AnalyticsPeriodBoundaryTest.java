package com.example.nzreceiptapp.domain.model;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** Pure-JVM regression coverage for the period contract used by Room and analytics. */
public class AnalyticsPeriodBoundaryTest {
    @Test public void leapDayAndNextMonthBoundary() {
        AnalyticsPeriod feb = AnalyticsPeriod.month(YearMonth.of(2024, 2));
        assertEquals(LocalDateTime.of(2024, 2, 1, 0, 0), feb.getStartInclusive());
        assertEquals(LocalDateTime.of(2024, 3, 1, 0, 0), feb.getEndExclusive());
        assertTrue(feb.contains(LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999999999)));
        assertFalse(feb.contains(LocalDateTime.of(2024, 3, 1, 0, 0)));
        assertFalse(feb.contains(null));
    }

    @Test public void dayAndYearUseInclusiveStartExclusiveEnd() {
        AnalyticsPeriod day = AnalyticsPeriod.day(LocalDate.of(2026, 9, 24));
        assertTrue(day.contains(LocalDateTime.of(2026, 9, 24, 0, 0)));
        assertFalse(day.contains(LocalDateTime.of(2026, 9, 25, 0, 0)));
        AnalyticsPeriod year = AnalyticsPeriod.year(2026);
        assertFalse(year.contains(LocalDateTime.of(2025, 12, 31, 23, 59, 59)));
        assertTrue(year.contains(LocalDateTime.of(2026, 12, 31, 23, 59, 59)));
        assertFalse(year.contains(LocalDateTime.of(2027, 1, 1, 0, 0)));
    }

    @Test public void invalidRangeIsRejected() {
        try {
            AnalyticsPeriod.between(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
            fail("Expected invalid range");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
