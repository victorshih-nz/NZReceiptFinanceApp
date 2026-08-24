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
import com.example.nzreceiptapp.presentation.viewmodel.HistoryViewModel;
import com.example.nzreceiptapp.R;

import androidx.navigation.Navigation;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private ReceiptAdapter receiptAdapter;

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

        setupRecyclerView();
        setupPagination();
        setupSwipeRefresh();
        observeViewModel();

        viewModel.loadData();
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

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(receiptAdapter);
    }

    private boolean pageSelectorProgrammatic = false;
    private boolean pageSizeProgrammatic = false;

    private void setupPagination() {
        binding.btnPrev.setOnClickListener(v -> viewModel.prevPage());
        binding.btnNext.setOnClickListener(v -> viewModel.nextPage());

        // Setup page size spinner if it exists in the layout
        if (binding.pageSizeSpinner != null) {
            binding.pageSizeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (pageSizeProgrammatic) return; // ignore programmatic changes
                    int[] sizes = {15, 30, 50};
                    int selected = sizes[position];
                    // avoid calling setPageSize if it's the same
                    Integer current = viewModel.getPageSize().getValue();
                    if (current != null && current == selected) return;
                    viewModel.setPageSize(selected);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        // page selector
        if (binding.pageSelector != null) {
            binding.pageSelector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    if (pageSelectorProgrammatic) return; // ignore programmatic selection changes
                    // position is 0-based; pages are 0-based internally
                    Integer cur = viewModel.getCurrentPage().getValue();
                    if (cur != null && cur == position) return; // no-op selecting current
                    viewModel.goToPage(position);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadData());
    }

    private void renderPagination(Integer curPage, Integer totalPagesVal) {
        int total = totalPagesVal != null ? totalPagesVal : 1;
        binding.txtTotalPages.setText("/ " + total);
        binding.btnPrev.setEnabled(curPage != null && curPage > 0);
        binding.btnNext.setEnabled(curPage != null && totalPagesVal != null && (curPage + 1 < totalPagesVal));

        // populate page selector entries
        if (binding.pageSelector != null && totalPagesVal != null) {
            java.util.List<String> pages = new java.util.ArrayList<>();
            for (int i = 1; i <= totalPagesVal; i++) pages.add(String.valueOf(i));
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, pages);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // avoid firing listener while programmatically changing adapter/selection
            pageSelectorProgrammatic = true;
            binding.pageSelector.setAdapter(adapter);

            Integer cur = curPage != null ? curPage : 0;
            if (cur >= 0 && cur < totalPagesVal) {
                binding.pageSelector.setSelection(cur);
            }
            pageSelectorProgrammatic = false;
        }
    }

    private void observeViewModel() {
        viewModel.getReceipts().observe(getViewLifecycleOwner(), receipts -> {
            receiptAdapter.submitList(receipts);
            binding.txtEmpty.setVisibility((receipts == null || receipts.isEmpty()) ? View.VISIBLE : View.GONE);
        });

        viewModel.getCurrentPage().observe(getViewLifecycleOwner(), page -> {
            Integer total = viewModel.getTotalPages().getValue();
            renderPagination(page, total);
        });

        viewModel.getPageSize().observe(getViewLifecycleOwner(), size -> {
            if (size != null && binding.pageSizeSpinner != null) {
                int[] sizes = {15, 30, 50};
                pageSizeProgrammatic = true;
                for (int i = 0; i < sizes.length; i++) {
                    if (sizes[i] == size) {
                        binding.pageSizeSpinner.setSelection(i);
                        break;
                    }
                }
                pageSizeProgrammatic = false;
            }
        });

        viewModel.getTotalPages().observe(getViewLifecycleOwner(), total -> {
            Integer cur = viewModel.getCurrentPage().getValue();
            renderPagination(cur, total);
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
