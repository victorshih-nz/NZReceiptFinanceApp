package com.example.nzreceiptapp.presentation.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nzreceiptapp.data.local.AppDatabase;
import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.repository.ReceiptRepositoryImpl;
import com.example.nzreceiptapp.databinding.FragmentReceiptDetailBinding;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;
import com.example.nzreceiptapp.presentation.adapter.ReceiptItemAdapter;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptDetailFragment extends Fragment {

    private FragmentReceiptDetailBinding binding;
    private ReceiptItemAdapter adapter;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReceiptDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String receiptId = getArguments() != null ? getArguments().getString("receiptId") : null;
        if (receiptId != null) {
            loadReceipt(receiptId);
        }
    }

    private void loadReceipt(String id) {
        // Since we don't have a ViewModel for details yet, let's use a simple thread for now
        // or we could add it to ViewModelFactory.
        new Thread(() -> {
            ReceiptDao dao = AppDatabase.getDatabase(requireContext()).receiptDao();
            IReceiptRepository repo = new ReceiptRepositoryImpl(dao);
            Receipt receipt = repo.getReceiptById(id);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (receipt != null) {
                        displayReceipt(receipt);
                    }
                });
            }
        }).start();
    }

    private void displayReceipt(Receipt receipt) {
        binding.txtDetailChain.setText(receipt.getStore().getChainName());
        binding.txtDetailBranch.setText(receipt.getStore().getBranchName());
        binding.txtDetailDate.setText(receipt.getPurchaseDate().format(DATE_FORMATTER));
        
        double total = receipt.getFinalPayableCents() / 100.0;
        binding.txtDetailTotal.setText(String.format(Locale.getDefault(), "$%.2f", total));

        adapter = new ReceiptItemAdapter();
        binding.rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvItems.setAdapter(adapter);
        adapter.submitList(receipt.getItems());

        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert);
        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
