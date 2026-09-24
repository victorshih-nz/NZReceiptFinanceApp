package com.example.nzreceiptapp.presentation.viewmodel;

import com.example.nzreceiptapp.domain.model.AnalyticsSummary;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable snapshot: chart selection and detail always describe the same period. */
public final class AnalyticsUiState {
    public enum Level { YEAR, MONTH, DAY }
    public enum Status { IDLE, LOADING, CONTENT, EMPTY, ERROR }

    private final Level level;
    private final Status status;
    private final int year;
    private final Integer selectedYear;
    private final Integer selectedMonth;
    private final Integer selectedDay;
    private final Map<Integer, Long> bars;
    private final AnalyticsSummary summary;
    private final String errorMessage;

    private AnalyticsUiState(Level level, Status status, int year,
                             Integer selectedYear, Integer selectedMonth, Integer selectedDay,
                             Map<Integer, Long> bars, AnalyticsSummary summary, String errorMessage) {
        this.level = level;
        this.status = status;
        this.year = year;
        this.selectedYear = selectedYear;
        this.selectedMonth = selectedMonth;
        this.selectedDay = selectedDay;
        this.bars = Collections.unmodifiableMap(new TreeMap<>(bars));
        this.summary = summary;
        this.errorMessage = errorMessage;
    }

    public static AnalyticsUiState initial(int currentYear) {
        return new AnalyticsUiState(Level.MONTH, Status.IDLE, currentYear, null, null, null,
                Collections.emptyMap(), null, null);
    }

    public AnalyticsUiState selection(Level nextLevel, int nextYear, Integer nextSelectedYear,
                                      Integer nextMonth, Integer nextDay) {
        return new AnalyticsUiState(nextLevel, Status.IDLE, nextYear, nextSelectedYear,
                nextMonth, nextDay, Collections.emptyMap(), null, null);
    }

    public AnalyticsUiState loading() {
        return new AnalyticsUiState(level, Status.LOADING, year, selectedYear,
                selectedMonth, selectedDay, Collections.emptyMap(), null, null);
    }

    public AnalyticsUiState loaded(Map<Integer, Long> newBars, AnalyticsSummary newSummary) {
        return new AnalyticsUiState(level, newSummary.isEmpty() ? Status.EMPTY : Status.CONTENT,
                year, selectedYear, selectedMonth, selectedDay, newBars, newSummary, null);
    }

    public AnalyticsUiState failed(String message) {
        return new AnalyticsUiState(level, Status.ERROR, year, selectedYear,
                selectedMonth, selectedDay, Collections.emptyMap(), null, message);
    }

    public Level getLevel() { return level; }
    public Status getStatus() { return status; }
    public int getYear() { return year; }
    public Integer getSelectedYear() { return selectedYear; }
    public Integer getSelectedMonth() { return selectedMonth; }
    public Integer getSelectedDay() { return selectedDay; }
    public Map<Integer, Long> getBars() { return bars; }
    public AnalyticsSummary getSummary() { return summary; }
    public String getErrorMessage() { return errorMessage; }
}
