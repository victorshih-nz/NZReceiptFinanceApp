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
import com.example.nzreceiptapp.databinding.FragmentReceiptDetailBinding;
import com.example.nzreceiptapp.di.ViewModelFactory;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.presentation.adapter.ReceiptItemAdapter;
import com.example.nzreceiptapp.presentation.viewmodel.ReceiptDetailViewModel;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptDetailFragment extends Fragment {

    private FragmentReceiptDetailBinding binding;
    private ReceiptItemAdapter adapter;
    private ReceiptDetailViewModel viewModel;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NzReceiptApplication app = (NzReceiptApplication) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(this, factory).get(ReceiptDetailViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReceiptDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ReceiptItemAdapter();
        binding.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvItems.setAdapter(adapter);
        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(
                ignored -> getParentFragmentManager().popBackStack()
        );

        viewModel.getReceipt().observe(getViewLifecycleOwner(), this::displayReceipt);
        viewModel.getErrorMessages().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        String receiptId = getArguments() != null ? getArguments().getString("receiptId") : null;
        viewModel.loadReceipt(receiptId);
    }

    private void displayReceipt(Receipt receipt) {
        binding.txtDetailChain.setText(receipt.getStore().getChainName());
        binding.txtDetailBranch.setText(receipt.getStore().getBranchName());
        binding.txtDetailDate.setText(receipt.getPurchaseDate().format(DATE_FORMATTER));
        
        double total = receipt.getFinalPayableCents() / 100.0;
        binding.txtDetailTotal.setText(String.format(Locale.getDefault(), "$%.2f", total));

        adapter.submitList(receipt.getItems());

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
