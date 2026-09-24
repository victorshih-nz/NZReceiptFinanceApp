package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.*;

import com.example.nzreceiptapp.domain.model.AnalyticsSummary;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Ensures chart and summary snapshots cannot be mutated by a rendering layer. */
public class AnalyticsUiStateContractTest {
    @Test public void snapshotCopiesBarsAndKeepsSelectedPeriod() {
        Map<Integer, Long> source = new TreeMap<>();
        source.put(1, 100L);
        AnalyticsUiState initial = AnalyticsUiState.initial(2026);
        AnalyticsUiState selected = initial.selection(
                AnalyticsUiState.Level.MONTH, 2026, null, 1, null);
        AnalyticsSummary summary = new AnalyticsSummary(90, 100, 1, 1, Collections.emptyList());
        AnalyticsUiState loaded = selected.loaded(source, summary);
        source.put(1, 900L);
        assertEquals(Long.valueOf(100), loaded.getBars().get(1));
        assertEquals(Integer.valueOf(1), loaded.getSelectedMonth());
        assertSame(summary, loaded.getSummary());
        assertEquals(AnalyticsUiState.Status.CONTENT, loaded.getStatus());
        try {
            loaded.getBars().put(2, 200L);
            fail("Bars must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test public void emptyAndErrorAreDistinctStates() {
        AnalyticsUiState initial = AnalyticsUiState.initial(2026);
        AnalyticsSummary empty = new AnalyticsSummary(0, 0, 0, 0, Collections.emptyList());
        AnalyticsUiState noData = initial.loaded(Collections.emptyMap(), empty);
        assertEquals(AnalyticsUiState.Status.EMPTY, noData.getStatus());
        assertNull(noData.getErrorMessage());
        AnalyticsUiState failed = initial.failed("DB unavailable");
        assertEquals(AnalyticsUiState.Status.ERROR, failed.getStatus());
        assertEquals("DB unavailable", failed.getErrorMessage());
        assertNull(failed.getSummary());
    }
}
