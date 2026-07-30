package com.example.nzreceiptapp.presentation.base;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.nzreceiptapp.data.local.AppDatabase;
import com.example.nzreceiptapp.data.local.dao.CategoryDao;
import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.ocr.MLKitOCRService;
import com.example.nzreceiptapp.data.parser.ParserProvider;
import com.example.nzreceiptapp.data.repository.CategoryRepositoryImpl;
import com.example.nzreceiptapp.data.repository.ReceiptRepositoryImpl;
import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsUseCase;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;
import com.example.nzreceiptapp.presentation.viewmodel.HistoryViewModel;
import com.example.nzreceiptapp.presentation.viewmodel.ScannerViewModel;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final Context context;

    public ViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        AppDatabase db = AppDatabase.getDatabase(context);
        CategoryDao categoryDao = db.categoryDao();
        ReceiptDao receiptDao = db.receiptDao();

        ICategoryRepository categoryRepo = new CategoryRepositoryImpl(categoryDao);
        IReceiptRepository receiptRepo = new ReceiptRepositoryImpl(receiptDao);

        CategoryClassifier classifier = new CategoryClassifier(categoryRepo);
        ParserProvider parserProvider = new ParserProvider(classifier);

        if (modelClass.isAssignableFrom(ScannerViewModel.class)) {
            return (T) new ScannerViewModel(
                    new MLKitOCRService(context),
                    new ParseReceiptUseCase(parserProvider),
                    new SaveReceiptUseCase(receiptRepo)
            );
        } else if (modelClass.isAssignableFrom(HistoryViewModel.class)) {
            return (T) new HistoryViewModel(
                    new GetReceiptsPagedUseCase(receiptRepo),
                    new GetAllItemsPagedUseCase(receiptRepo),
                    new DeleteReceiptUseCase(receiptRepo)
            );
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
