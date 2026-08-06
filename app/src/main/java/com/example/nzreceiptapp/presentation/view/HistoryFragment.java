package com.example.nzreceiptapp.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.nzreceiptapp.presentation.viewmodel.HistoryViewModel;
import com.google.android.material.tabs.TabLayout;
import com.example.nzreceiptapp.R;

import androidx.navigation.Navigation;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private ReceiptAdapter receiptAdapter;
    private ReceiptItemSummaryAdapter itemsAdapter;

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
                    viewModel.setViewMode(HistoryViewModel.ViewMode.RECEIPTS);
                } else {
                    viewModel.setViewMode(HistoryViewModel.ViewMode.ALL_ITEMS);
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
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadData());
    }

    private void observeViewModel() {
        viewModel.getViewMode().observe(getViewLifecycleOwner(), mode -> {
            if (mode == HistoryViewModel.ViewMode.RECEIPTS) {
                binding.recyclerView.setAdapter(receiptAdapter);
            } else {
                binding.recyclerView.setAdapter(itemsAdapter);
            }
        });

        viewModel.getReceipts().observe(getViewLifecycleOwner(), receipts -> {
            if (viewModel.getViewMode().getValue() == HistoryViewModel.ViewMode.RECEIPTS) {
                receiptAdapter.submitList(receipts);
                binding.txtEmpty.setVisibility((receipts == null || receipts.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getAllItems().observe(getViewLifecycleOwner(), items -> {
            if (viewModel.getViewMode().getValue() == HistoryViewModel.ViewMode.ALL_ITEMS) {
                itemsAdapter.submitList(items);
                binding.txtEmpty.setVisibility((items == null || items.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getCurrentPage().observe(getViewLifecycleOwner(), page -> {
            binding.txtPage.setText("Page " + (page + 1));
            binding.btnPrev.setEnabled(page > 0);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.swipeRefresh.setRefreshing(isLoading != null && isLoading);
        });

        viewModel.getErrorMessages().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
