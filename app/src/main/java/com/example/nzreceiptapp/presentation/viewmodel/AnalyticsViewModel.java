package com.example.nzreceiptapp.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.nzreceiptapp.domain.model.AnalyticsPeriod;
import com.example.nzreceiptapp.domain.model.AnalyticsSummary;
import com.example.nzreceiptapp.domain.usecase.GetAnalyticsUseCase;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/** Owns period selection and runs all database work off the main thread. */
public final class AnalyticsViewModel extends ViewModel {
    private final GetAnalyticsUseCase getAnalytics;
    private final Executor ioExecutor;
    private final AtomicLong requestSequence = new AtomicLong();
    private final MutableLiveData<AnalyticsUiState> uiState = new MutableLiveData<>();
    private AnalyticsUiState current;
    private boolean hasLoaded;

    public AnalyticsViewModel(GetAnalyticsUseCase getAnalytics, Executor ioExecutor, Clock clock) {
        this.getAnalytics = getAnalytics;
        this.ioExecutor = ioExecutor;
        current = AnalyticsUiState.initial(LocalDate.now(clock).getYear());
        uiState.setValue(current);
    }

    public LiveData<AnalyticsUiState> getUiState() { return uiState; }

    /** Called from the Fragment once; retained ViewModels do not load twice. */
    public void loadInitialData() {
        if (!hasLoaded) load();
    }

    public void refresh() { load(); }

    public void retry() {
        if (current.getStatus() == AnalyticsUiState.Status.ERROR) load();
    }

    public void showYears() {
        if (current.getLevel() == AnalyticsUiState.Level.YEAR) return;
        change(current.selection(AnalyticsUiState.Level.YEAR, current.getYear(), null, null, null));
    }

    public void showMonths(int year) {
        change(current.selection(AnalyticsUiState.Level.MONTH, year, null, null, null));
    }

    /** Single tap changes the selected total but does not drill down. */
    public void selectYear(int year) {
        if (current.getLevel() != AnalyticsUiState.Level.YEAR) return;
        change(current.selection(AnalyticsUiState.Level.YEAR, year, year, null, null));
    }

    /** Invoked by the View's actual double-tap detection (Job 3). */
    public void drillDownYear(int year) {
        if (current.getLevel() != AnalyticsUiState.Level.YEAR) return;
        showMonths(year);
    }

    public void selectMonth(int month) {
        if (current.getLevel() != AnalyticsUiState.Level.MONTH || month < 1 || month > 12) return;
        change(current.selection(AnalyticsUiState.Level.MONTH, current.getYear(), null, month, null));
    }

    public void drillDownMonth(int month) {
        if (current.getLevel() != AnalyticsUiState.Level.MONTH || month < 1 || month > 12) return;
        change(current.selection(AnalyticsUiState.Level.DAY, current.getYear(), null, month, null));
    }

    public void selectDay(int day) {
        if (current.getLevel() != AnalyticsUiState.Level.DAY) return;
        int month = current.getSelectedMonth();
        if (day < 1 || day > YearMonth.of(current.getYear(), month).lengthOfMonth()) return;
        change(current.selection(AnalyticsUiState.Level.DAY, current.getYear(), null, month, day));
    }

    public void back() {
        if (current.getLevel() == AnalyticsUiState.Level.DAY) {
            change(current.selection(AnalyticsUiState.Level.MONTH, current.getYear(), null,
                    current.getSelectedMonth(), null));
        } else if (current.getLevel() == AnalyticsUiState.Level.MONTH) {
            change(current.selection(AnalyticsUiState.Level.YEAR, current.getYear(),
                    current.getYear(), null, null));
        }
    }

    private void change(AnalyticsUiState next) {
        current = next;
        load();
    }

    private void load() {
        final long requestId = requestSequence.incrementAndGet();
        hasLoaded = true;
        final AnalyticsUiState request = current.loading();
        current = request;
        uiState.setValue(request);
        ioExecutor.execute(() -> {
            try {
                Map<Integer, Long> bars;
                AnalyticsPeriod period;
                if (request.getLevel() == AnalyticsUiState.Level.YEAR) {
                    bars = new TreeMap<>(getAnalytics.getYearlyTotals());
                    bars.putIfAbsent(request.getYear(), 0L);
                    period = AnalyticsPeriod.year(request.getYear());
                } else if (request.getLevel() == AnalyticsUiState.Level.MONTH) {
                    bars = getAnalytics.getMonthlyTotals(request.getYear());
                    period = request.getSelectedMonth() == null
                            ? AnalyticsPeriod.year(request.getYear())
                            : AnalyticsPeriod.month(YearMonth.of(request.getYear(), request.getSelectedMonth()));
                } else {
                    YearMonth month = YearMonth.of(request.getYear(), request.getSelectedMonth());
                    bars = getAnalytics.getDailyTotals(month);
                    period = request.getSelectedDay() == null
                            ? AnalyticsPeriod.month(month)
                            : AnalyticsPeriod.day(month.atDay(request.getSelectedDay()));
                }
                AnalyticsSummary summary = getAnalytics.execute(period);
                if (requestId != requestSequence.get()) return;
                publish(request.loaded(bars, summary));
            } catch (Exception e) {
                if (requestId != requestSequence.get()) return;
                publish(request.failed("Unable to load analytics. Please retry."));
            }
        });
    }

    private void publish(AnalyticsUiState state) {
        current = state;
        uiState.postValue(state);
    }

    @Override protected void onCleared() {
        requestSequence.incrementAndGet();
        super.onCleared();
    }
}
