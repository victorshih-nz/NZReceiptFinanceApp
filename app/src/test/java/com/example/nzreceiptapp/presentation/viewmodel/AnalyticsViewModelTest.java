package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.nzreceiptapp.domain.model.AnalyticsSummary;
import com.example.nzreceiptapp.domain.model.AnalyticsPeriod;
import com.example.nzreceiptapp.domain.usecase.GetAnalyticsUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnalyticsViewModelTest {
    @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
    private GetAnalyticsUseCase useCase;
    private AnalyticsViewModel vm;
    private ControlledExecutor executor;

    @Before public void setUp() {
        useCase = mock(GetAnalyticsUseCase.class);
        executor = new ControlledExecutor();
        vm = new AnalyticsViewModel(useCase, executor,
                Clock.fixed(Instant.parse("2026-09-24T00:00:00Z"), ZoneOffset.UTC));
        when(useCase.getMonthlyTotals(anyInt())).thenReturn(Collections.singletonMap(1, 300L));
        when(useCase.getDailyTotals(any(YearMonth.class))).thenReturn(Collections.singletonMap(5, 300L));
        when(useCase.getYearlyTotals()).thenReturn(Collections.singletonMap(2025, 200L));
        when(useCase.execute(any(AnalyticsPeriod.class))).thenReturn(summary(1, 300));
    }

    @Test public void initialLoadsOnceAndDefaultsToCurrentYearMonthlyChart() {
        assertEquals(AnalyticsUiState.Status.IDLE, state().getStatus());
        vm.loadInitialData();
        vm.loadInitialData();
        assertEquals(1, executor.size());
        assertEquals(AnalyticsUiState.Status.LOADING, state().getStatus());
        executor.runAt(0);
        assertEquals(AnalyticsUiState.Status.CONTENT, state().getStatus());
        assertEquals(AnalyticsUiState.Level.MONTH, state().getLevel());
        assertEquals(2026, state().getYear());
        assertEquals(Long.valueOf(300), state().getBars().get(1));
        verify(useCase).execute(any(AnalyticsPeriod.class));
    }

    @Test public void singleTapMonthUpdatesSummaryButDoesNotDrill() {
        vm.loadInitialData(); executor.runAt(0);
        vm.selectMonth(1);
        assertEquals(AnalyticsUiState.Level.MONTH, state().getLevel());
        assertEquals(Integer.valueOf(1), state().getSelectedMonth());
        executor.runAt(0);
        assertEquals(AnalyticsUiState.Level.MONTH, state().getLevel());
        assertEquals(1, state().getSummary().getReceiptCount());
    }

    @Test public void drillToDayTapDayAndBackPreserveMonth() {
        vm.drillDownMonth(2);
        assertEquals(AnalyticsUiState.Level.DAY, state().getLevel());
        assertEquals(Integer.valueOf(2), state().getSelectedMonth());
        executor.runAt(0);
        vm.selectDay(5); executor.runAt(0);
        assertEquals(Integer.valueOf(5), state().getSelectedDay());
        vm.back(); executor.runAt(0);
        assertEquals(AnalyticsUiState.Level.MONTH, state().getLevel());
        assertEquals(Integer.valueOf(2), state().getSelectedMonth());
        assertNull(state().getSelectedDay());
    }

    @Test public void annualSelectionAndDrillFollowExplicitActions() {
        vm.showYears(); executor.runAt(0);
        vm.selectYear(2025); executor.runAt(0);
        assertEquals(AnalyticsUiState.Level.YEAR, state().getLevel());
        assertEquals(Integer.valueOf(2025), state().getSelectedYear());
        vm.drillDownYear(2025); executor.runAt(0);
        assertEquals(AnalyticsUiState.Level.MONTH, state().getLevel());
        assertEquals(2025, state().getYear());
        vm.back(); executor.runAt(0);
        assertEquals(AnalyticsUiState.Level.YEAR, state().getLevel());
        assertEquals(Integer.valueOf(2025), state().getSelectedYear());
    }

    @Test public void staleJobCannotOverwriteNewerSelection() {
        vm.loadInitialData();
        vm.selectMonth(2);
        executor.runAt(1);
        executor.runAt(0);
        assertEquals(Integer.valueOf(2), state().getSelectedMonth());
    }

    @Test public void errorCanRetryAndEmptyIsNotError() {
        when(useCase.getMonthlyTotals(anyInt())).thenThrow(new IllegalStateException("DB"))
                .thenReturn(Collections.singletonMap(1, 0L));
        when(useCase.execute(any(AnalyticsPeriod.class))).thenReturn(summary(0, 0));
        vm.loadInitialData(); executor.runAt(0);
        assertEquals(AnalyticsUiState.Status.ERROR, state().getStatus());
        assertNotNull(state().getErrorMessage());
        vm.retry(); executor.runAt(0);
        assertEquals(AnalyticsUiState.Status.EMPTY, state().getStatus());
        assertNull(state().getErrorMessage());
    }

    @Test public void invalidDayAndMonthAreIgnored() {
        vm.selectMonth(13);
        assertEquals(0, executor.size());
        vm.drillDownMonth(2); executor.runAt(0);
        vm.selectDay(30); // Feb 2026 has 28 days
        assertEquals(0, executor.size());
    }

    private AnalyticsUiState state() { return vm.getUiState().getValue(); }
    private static AnalyticsSummary summary(int count, long cents) {
        return new AnalyticsSummary(cents, cents, count, count, Collections.emptyList());
    }
    private static final class ControlledExecutor implements java.util.concurrent.Executor {
        final List<Runnable> queue = new ArrayList<>();
        @Override public void execute(Runnable task) { queue.add(task); }
        int size() { return queue.size(); }
        void runAt(int i) { queue.remove(i).run(); }
    }
}
