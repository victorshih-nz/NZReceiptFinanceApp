package com.example.nzreceiptapp.di;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.nzreceiptapp.presentation.viewmodel.HistoryViewModel;
import com.example.nzreceiptapp.presentation.viewmodel.ReceiptDetailViewModel;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerViewModel;

/** Creates ViewModels from dependencies owned by the application container. */
public final class ViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer container;

    public ViewModelFactory(AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ScannerViewModel.class)) {
            return (T) new ScannerViewModel(
                    container.ocrService(),
                    container.parseReceiptUseCase(),
                    container.saveReceiptUseCase(),
                    container.ioExecutor()
            );
        }
        if (modelClass.isAssignableFrom(HistoryViewModel.class)) {
            return (T) new HistoryViewModel(
                    container.getReceiptsPagedUseCase(),
                    container.getAllItemsPagedUseCase(),
                    container.deleteReceiptUseCase(),
                    container.ioExecutor()
            );
        }
        if (modelClass.isAssignableFrom(ReceiptDetailViewModel.class)) {
            return (T) new ReceiptDetailViewModel(
                    container.getReceiptByIdUseCase(),
                    container.ioExecutor()
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
