package com.example.nzreceiptapp.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.nzreceiptapp.NzReceiptApplication;
import com.example.nzreceiptapp.databinding.FragmentAnalyticsBinding;
import com.example.nzreceiptapp.di.ViewModelFactory;
import com.example.nzreceiptapp.domain.model.AnalyticsSummary;
import com.example.nzreceiptapp.domain.model.CategorySpending;
import com.example.nzreceiptapp.presentation.viewmodel.AnalyticsUiState;
import com.example.nzreceiptapp.presentation.viewmodel.AnalyticsViewModel;
import com.google.android.material.color.MaterialColors;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Navigation destination: all data and selection come from AnalyticsViewModel. */
public final class AnalyticsFragment extends Fragment {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
    private FragmentAnalyticsBinding binding;
    private AnalyticsViewModel viewModel;
    private AnalyticsUiState lastState;
    private String lastChartPeriod;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NzReceiptApplication app = (NzReceiptApplication) requireActivity().getApplication();
        viewModel = new ViewModelProvider(this, new ViewModelFactory(app.getAppContainer()))
                .get(AnalyticsViewModel.class);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnBack.setOnClickListener(v -> viewModel.back());
        binding.btnYears.setOnClickListener(v -> viewModel.showYears());
        binding.btnPrevYear.setOnClickListener(v -> changeYear(-1));
        binding.btnNextYear.setOnClickListener(v -> changeYear(1));
        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
        binding.swipeRefresh.setOnRefreshListener(viewModel::refresh);
        binding.chart.setListener(new AnalyticsBarChartView.Listener() {
            @Override public void onSelect(int key) {
                if (lastState == null) return;
                switch (lastState.getLevel()) {
                    case YEAR: viewModel.selectYear(key); break;
                    case MONTH: viewModel.selectMonth(key); break;
                    case DAY: viewModel.selectDay(key); break;
                }
            }
            @Override public void onDrillDown(int key) {
                if (lastState == null) return;
                switch (lastState.getLevel()) {
                    case YEAR: viewModel.drillDownYear(key); break;
                    case MONTH: viewModel.drillDownMonth(key); break;
                    case DAY: viewModel.selectDay(key); break;
                }
            }
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.loadInitialData();
    }

    private void changeYear(int delta) {
        if (lastState == null || lastState.getLevel() != AnalyticsUiState.Level.MONTH) return;
        int next = lastState.getYear() + delta;
        if (next >= 1 && next <= 9999) viewModel.showMonths(next);
    }

    private void render(AnalyticsUiState state) {
        if (binding == null || state == null) return;
        lastState = state;
        boolean yearMode = state.getLevel() == AnalyticsUiState.Level.YEAR;
        boolean monthMode = state.getLevel() == AnalyticsUiState.Level.MONTH;
        binding.btnBack.setVisibility(state.getLevel() == AnalyticsUiState.Level.DAY
                ? View.VISIBLE : View.GONE);
        binding.btnYears.setVisibility(yearMode ? View.GONE : View.VISIBLE);
        binding.btnPrevYear.setVisibility(monthMode ? View.VISIBLE : View.GONE);
        binding.btnNextYear.setVisibility(monthMode ? View.VISIBLE : View.GONE);
        if (yearMode) binding.txtPeriod.setText("All years");
        else if (monthMode) binding.txtPeriod.setText(String.valueOf(state.getYear()));
        else binding.txtPeriod.setText(YearMonth.of(state.getYear(), state.getSelectedMonth()).format(MONTH_FORMAT));
        boolean loading = state.getStatus() == AnalyticsUiState.Status.LOADING;
        boolean error = state.getStatus() == AnalyticsUiState.Status.ERROR;
        boolean ready = state.getStatus() == AnalyticsUiState.Status.CONTENT
                || state.getStatus() == AnalyticsUiState.Status.EMPTY;
        binding.progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        binding.errorContainer.setVisibility(error ? View.VISIBLE : View.GONE);
        if (error) binding.txtError.setText(state.getErrorMessage());
        binding.chartScroll.setVisibility(ready ? View.VISIBLE : View.GONE);
        binding.txtChartHint.setVisibility(ready ? View.VISIBLE : View.GONE);
        binding.txtEmpty.setVisibility(state.getStatus() == AnalyticsUiState.Status.EMPTY
                ? View.VISIBLE : View.GONE);
        binding.summaryContainer.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (!ready) return;

        Integer selected = yearMode ? state.getSelectedYear()
                : monthMode ? state.getSelectedMonth() : state.getSelectedDay();
        // Wait for the actual horizontal viewport width; display metrics include system insets.
        String chartPeriod = state.getLevel() + ":" + state.getYear() + ":"
                + state.getSelectedMonth() + ":" + selected;
        // A refresh may add the first receipt without changing the selected period.
        if (selected == null) chartPeriod += ":first=" + firstSpendingKey(state.getBars());
        boolean periodChanged = !chartPeriod.equals(lastChartPeriod);
        if (periodChanged) lastChartPeriod = chartPeriod;
        final FragmentAnalyticsBinding currentBinding = binding;
        // Resize while the viewport is measured, then position after the chart's new layout.
        // A nested post() alone can execute before the HorizontalScrollView has its new range.
        currentBinding.chartScroll.post(() -> {
            if (binding != currentBinding || lastState != state) return;
            int viewport = currentBinding.chartScroll.getWidth()
                    - currentBinding.chartScroll.getPaddingLeft()
                    - currentBinding.chartScroll.getPaddingRight();
            if (viewport <= 0) return;
            currentBinding.chart.setData(state.getBars(), state.getLevel(), selected, viewport);
            if (!periodChanged) return; // Do not undo manual horizontal scrolling.
            int target = selected != null ? selected : firstSpendingKey(state.getBars());
            // The scroll range is valid only after the next child measurement/layout.
            currentBinding.chartScroll.postOnAnimation(() -> {
                if (binding != currentBinding || lastState != state) return;
                int centre = target == -1 ? -1 : currentBinding.chart.getCentreXForKey(target);
                int available = currentBinding.chartScroll.getWidth()
                        - currentBinding.chartScroll.getPaddingLeft()
                        - currentBinding.chartScroll.getPaddingRight();
                int content = Math.max(currentBinding.chart.getWidth(),
                        currentBinding.chart.getMeasuredWidth());
                int maxScroll = Math.max(0, content - available);
                int x = centre < 0 ? 0 : Math.max(0,
                        Math.min(maxScroll, centre - available / 2));
                currentBinding.chartScroll.scrollTo(x, 0);
                currentBinding.chart.invalidate();
            });
        });
        AnalyticsSummary summary = state.getSummary();
        binding.txtSelectedPeriod.setText(selectedPeriodText(state));
        binding.txtTotal.setText(money(summary.getTotalSpendingCents()));
        binding.txtCounts.setText(String.format(Locale.getDefault(), "%d receipts · %d items",
                summary.getReceiptCount(), summary.getItemCount()));
        renderCategories(summary);
    }

    private static int firstSpendingKey(Map<Integer, Long> bars) {
        int first = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Long> entry : bars.entrySet()) {
            if (entry.getValue() > 0 && entry.getKey() < first) first = entry.getKey();
        }
        return first == Integer.MAX_VALUE ? -1 : first;
    }

    private static String selectedPeriodText(AnalyticsUiState state) {
        int year = state.getYear();
        if (state.getLevel() == AnalyticsUiState.Level.YEAR) {
            return state.getSelectedYear() == null ? "Year " + year : "Year " + state.getSelectedYear();
        }
        if (state.getLevel() == AnalyticsUiState.Level.MONTH) {
            return state.getSelectedMonth() == null ? "Full year " + year
                    : YearMonth.of(year, state.getSelectedMonth()).format(MONTH_FORMAT);
        }
        YearMonth month = YearMonth.of(year, state.getSelectedMonth());
        return state.getSelectedDay() == null ? month.format(MONTH_FORMAT)
                : month.atDay(state.getSelectedDay()).toString();
    }

    private void renderCategories(AnalyticsSummary summary) {
        binding.categoryList.removeAllViews();
        long denominator = summary.getCategoryTotalCents();
        int primary = MaterialColors.getColor(binding.categoryList,
                com.google.android.material.R.attr.colorPrimary);
        for (CategorySpending category : summary.getCategories()) {
            String name = category.getCategoryName();
            if (name == null || name.trim().isEmpty()) name = "Uncategorized";
            long amount = category.getTotalAmountCents();
            double percent = denominator > 0 ? 100.0 * amount / denominator : 0;
            TextView label = new TextView(requireContext());
            label.setText(String.format(Locale.getDefault(), "%s  ·  %s  ·  %.1f%%",
                    name, money(amount), percent));
            label.setPadding(0, dp(12), 0, dp(4));
            binding.categoryList.addView(label);
            ProgressBar bar = new ProgressBar(requireContext(), null,
                    android.R.attr.progressBarStyleHorizontal);
            bar.setMax(1000);
            bar.setProgress((int) Math.round(Math.max(0, Math.min(100, percent)) * 10));
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(primary));
            binding.categoryList.addView(bar, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        }
        binding.txtCategoryNote.setVisibility(summary.getCategories().isEmpty()
                ? View.GONE : View.VISIBLE);
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private static String money(long cents) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-NZ"));
        return formatter.format(BigDecimal.valueOf(cents, 2));
    }

    @Override public void onDestroyView() {
        if (binding != null) binding.chart.setListener(null);
        binding = null;
        lastState = null;
        lastChartPeriod = null;
        super.onDestroyView();
    }
}
