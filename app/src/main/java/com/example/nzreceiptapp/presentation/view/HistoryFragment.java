package com.example.nzreceiptapp.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nzreceiptapp.NzReceiptApplication;
import com.example.nzreceiptapp.databinding.FragmentHistoryBinding;
import com.example.nzreceiptapp.di.ViewModelFactory;
import com.example.nzreceiptapp.presentation.adapter.ReceiptAdapter;
import com.example.nzreceiptapp.presentation.adapter.ReceiptItemSummaryAdapter;
import com.example.nzreceiptapp.presentation.viewmodel.HistoryUiState;
import com.example.nzreceiptapp.presentation.viewmodel.HistoryViewModel;
import com.google.android.material.tabs.TabLayout;
import com.example.nzreceiptapp.R;

import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private ReceiptAdapter receiptAdapter;
    private ReceiptItemSummaryAdapter itemsAdapter;
    private boolean updatingPageSpinner;
    private boolean updatingPageSizeSpinner;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NzReceiptApplication app = (NzReceiptApplication) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(this, factory).get(HistoryViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupTabs();
        setupRecyclerView();
        setupPagination();
        setupSwipeRefresh();
        observeViewModel();

        // Initial load
        viewModel.loadData();
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    viewModel.setViewMode(HistoryUiState.ViewMode.RECEIPTS);
                } else {
                    viewModel.setViewMode(HistoryUiState.ViewMode.ALL_ITEMS);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        receiptAdapter = new ReceiptAdapter(
            receipt -> {
                Bundle args = new Bundle();
                args.putString("receiptId", receipt.getId());
                Navigation.findNavController(requireView()).navigate(R.id.receiptDetailFragment, args);
            },
            receipt -> {
                viewModel.deleteReceipt(receipt.getId());
                Toast.makeText(getContext(), "Receipt deleted", Toast.LENGTH_SHORT).show();
            }
        );
        itemsAdapter = new ReceiptItemSummaryAdapter();
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter will be switched in observeViewModel
    }

    private void setupPagination() {
        binding.btnPrev.setOnClickListener(v -> viewModel.prevPage());
        binding.btnNext.setOnClickListener(v -> viewModel.nextPage());

        ArrayAdapter<CharSequence> pageSizeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.history_page_size_choices,
                android.R.layout.simple_spinner_item);
        pageSizeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        updatingPageSizeSpinner = true;
        binding.spinnerPageSize.setAdapter(pageSizeAdapter);
        binding.spinnerPageSize.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {
                        if (!updatingPageSizeSpinner) {
                            int pageSize = Integer.parseInt(
                                    parent.getItemAtPosition(position).toString());
                            viewModel.setPageSize(pageSize);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });
        binding.spinnerPageSize.post(() -> updatingPageSizeSpinner = false);

        binding.spinnerPage.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View view,
                                               int position,
                                               long id) {
                        if (!updatingPageSpinner) {
                            viewModel.goToPage(position + 1);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadData());
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(
                getViewLifecycleOwner(), this::renderState);
    }

    private void renderState(HistoryUiState state) {
        boolean receiptsMode =
                state.getViewMode() == HistoryUiState.ViewMode.RECEIPTS;
        if (receiptsMode) {
            binding.recyclerView.setAdapter(receiptAdapter);
            receiptAdapter.submitList(state.getReceipts());
        } else {
            binding.recyclerView.setAdapter(itemsAdapter);
            itemsAdapter.submitList(state.getAllItems());
        }

        binding.swipeRefresh.setRefreshing(state.isLoading());
        binding.txtEmpty.setVisibility(
                state.getLoadState() == HistoryUiState.LoadState.EMPTY
                        && state.isActiveContentEmpty()
                        ? View.VISIBLE : View.GONE);
        renderPagingState(state.getActivePaging(), state.isLoading());
        renderPageSize(state.getActivePaging().getPageSize(), state.isLoading());

        int tabPosition = receiptsMode ? 0 : 1;
        TabLayout.Tab selectedTab = binding.tabLayout.getTabAt(tabPosition);
        if (selectedTab != null && !selectedTab.isSelected()) {
            selectedTab.select();
        }

        if (state.getErrorMessage() != null) {
            Toast.makeText(
                    getContext(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPageSize(int pageSize, boolean loading) {
        int selectedPosition;
        if (pageSize == 30) {
            selectedPosition = 1;
        } else if (pageSize == 50) {
            selectedPosition = 2;
        } else {
            selectedPosition = 0;
        }

        if (binding.spinnerPageSize.getSelectedItemPosition()
                != selectedPosition) {
            updatingPageSizeSpinner = true;
            binding.spinnerPageSize.setSelection(selectedPosition, false);
            binding.spinnerPageSize.post(
                    () -> updatingPageSizeSpinner = false);
        }
        binding.spinnerPageSize.setEnabled(!loading);
    }

    private void renderPagingState(HistoryUiState.PagingState state,
                                   boolean loading) {
        binding.txtPage.setText(getString(
                R.string.page_indicator,
                state.getCurrentPage(),
                state.getTotalPages()));
        binding.btnPrev.setEnabled(state.hasPrevious() && !loading);
        binding.btnNext.setEnabled(state.hasNext() && !loading);

        List<String> pages = new ArrayList<>();
        for (int page = 1; page <= state.getTotalPages(); page++) {
            pages.add(String.valueOf(page));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, pages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        updatingPageSpinner = true;
        binding.spinnerPage.setAdapter(adapter);
        binding.spinnerPage.setSelection(state.getCurrentPage() - 1, false);
        binding.spinnerPage.setEnabled(state.getTotalPages() > 1 && !loading);
        binding.spinnerPage.post(() -> updatingPageSpinner = false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
