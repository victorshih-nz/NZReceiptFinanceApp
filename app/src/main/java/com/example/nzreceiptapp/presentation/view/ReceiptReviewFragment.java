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
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.nzreceiptapp.NzReceiptApplication;
import com.example.nzreceiptapp.databinding.FragmentReceiptReviewBinding;
import com.example.nzreceiptapp.di.ViewModelFactory;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.presentation.adapter.ReceiptReviewAdapter;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerUiState;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerViewModel;

import java.util.List;
import java.util.Locale;

/** Lets the user verify OCR output before any database write occurs. */
public final class ReceiptReviewFragment extends Fragment {
    private FragmentReceiptReviewBinding binding;
    private ScannerViewModel viewModel;
    private ReceiptReviewAdapter adapter;
    private String boundReceiptId;
    private Long printedTotalCents;
    private boolean handledSaved;
    private String lastShownError;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NzReceiptApplication app = (NzReceiptApplication) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(requireActivity(), factory)
                .get(ScannerViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReceiptReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new ReceiptReviewAdapter(this::refreshCalculatedTotal);
        binding.rvReviewItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReviewItems.setAdapter(adapter);
        binding.toolbar.setNavigationOnClickListener(ignored -> cancelReview());
        binding.btnCancel.setOnClickListener(ignored -> cancelReview());
        binding.btnAddItem.setOnClickListener(ignored -> adapter.addEmptyItem());
        binding.btnSaveReceipt.setOnClickListener(ignored -> saveReceipt());
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(ScannerUiState state) {
        if (state == null) return;
        Receipt draft = state.getDraft();
        if (draft != null && !draft.getId().equals(boundReceiptId)) {
            boundReceiptId = draft.getId();
            printedTotalCents = draft.getPrintedTotalCents();
            binding.editChain.setText(draft.getStore().getChainName());
            binding.editBranch.setText(draft.getStore().getBranchName());
            binding.txtRawOcr.setText(draft.getRawOcrText());
            adapter.submit(draft.getItems(), state.getCategories());
            refreshCalculatedTotal();
        }

        boolean saving = state.getPhase() == ScannerUiState.Phase.SAVING_RECEIPT;
        binding.progressSave.setVisibility(saving ? View.VISIBLE : View.GONE);
        binding.btnSaveReceipt.setEnabled(!saving && draft != null);
        binding.btnCancel.setEnabled(!saving);
        binding.btnAddItem.setEnabled(!saving);

        if (state.getErrorMessage() != null
                && !state.getErrorMessage().equals(lastShownError)) {
            lastShownError = state.getErrorMessage();
            Toast.makeText(requireContext(), state.getErrorMessage(), Toast.LENGTH_LONG).show();
        }
        if (state.getPhase() == ScannerUiState.Phase.SAVED && !handledSaved) {
            handledSaved = true;
            Toast.makeText(requireContext(), "Receipt saved", Toast.LENGTH_SHORT).show();
            viewModel.reset();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }

    private void saveReceipt() {
        try {
            List<ReceiptItem> editedItems = adapter.buildReceiptItems();
            viewModel.saveReviewedReceipt(
                    binding.editChain.getText().toString(),
                    binding.editBranch.getText().toString(),
                    editedItems);
        } catch (IllegalArgumentException exception) {
            Toast.makeText(requireContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshCalculatedTotal() {
        if (binding == null || adapter == null) return;
        try {
            long calculated = 0;
            for (ReceiptItem item : adapter.buildReceiptItems()) {
                calculated += item.getFinalSubtotalCents();
            }
            binding.txtCalculatedTotal.setText(money(calculated));
            if (printedTotalCents == null) {
                binding.txtPrintedTotal.setText("Not recognised");
                binding.txtTotalWarning.setText("Check the calculated total before saving.");
                binding.txtTotalWarning.setVisibility(View.VISIBLE);
            } else {
                binding.txtPrintedTotal.setText(money(printedTotalCents));
                long difference = Math.abs(printedTotalCents - calculated);
                binding.txtTotalWarning.setVisibility(difference > 1 ? View.VISIBLE : View.GONE);
                binding.txtTotalWarning.setText(
                        difference > 1 ? "Printed and calculated totals do not match." : "");
            }
        } catch (IllegalArgumentException ignored) {
            binding.txtCalculatedTotal.setText("Invalid item data");
            binding.txtTotalWarning.setText("Fix invalid item values before saving.");
            binding.txtTotalWarning.setVisibility(View.VISIBLE);
        }
    }

    private String money(long cents) {
        return String.format(Locale.getDefault(), "$%.2f", cents / 100.0);
    }

    private void cancelReview() {
        viewModel.discardDraft();
        Navigation.findNavController(requireView()).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
