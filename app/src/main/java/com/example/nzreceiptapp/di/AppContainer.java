package com.example.nzreceiptapp.di;

import android.content.Context;

import com.example.nzreceiptapp.data.local.AppDatabase;
import com.example.nzreceiptapp.data.ocr.MLKitOCRService;
import com.example.nzreceiptapp.data.parser.ParserProvider;
import com.example.nzreceiptapp.data.repository.CategoryRepositoryImpl;
import com.example.nzreceiptapp.data.repository.ReceiptRepositoryImpl;
import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Composition root for the app.
 *
 * This is the only place where concrete data implementations are connected to
 * domain interfaces. Presentation classes receive ready-to-use dependencies.
 */
public final class AppContainer {
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);

    private final IOCRService ocrService;
    private final ParseReceiptUseCase parseReceiptUseCase;
    private final SaveReceiptUseCase saveReceiptUseCase;
    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final GetAllItemsPagedUseCase getAllItemsPagedUseCase;
    private final GetReceiptByIdUseCase getReceiptByIdUseCase;
    private final DeleteReceiptUseCase deleteReceiptUseCase;

    public AppContainer(Context context) {
        Context applicationContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(applicationContext);

        ICategoryRepository categoryRepository =
                new CategoryRepositoryImpl(database.categoryDao());
        IReceiptRepository receiptRepository =
                new ReceiptRepositoryImpl(database.receiptDao());

        CategoryClassifier classifier = new CategoryClassifier(categoryRepository);
        ParserProvider parserProvider = new ParserProvider(classifier);

        ocrService = new MLKitOCRService(applicationContext);
        parseReceiptUseCase = new ParseReceiptUseCase(parserProvider);
        saveReceiptUseCase = new SaveReceiptUseCase(receiptRepository);
        getReceiptsPagedUseCase = new GetReceiptsPagedUseCase(receiptRepository);
        getAllItemsPagedUseCase = new GetAllItemsPagedUseCase(receiptRepository);
        getReceiptByIdUseCase = new GetReceiptByIdUseCase(receiptRepository);
        deleteReceiptUseCase = new DeleteReceiptUseCase(receiptRepository);
    }

    Executor ioExecutor() {
        return ioExecutor;
    }

    IOCRService ocrService() {
        return ocrService;
    }

    ParseReceiptUseCase parseReceiptUseCase() {
        return parseReceiptUseCase;
    }

    SaveReceiptUseCase saveReceiptUseCase() {
        return saveReceiptUseCase;
    }

    GetReceiptsPagedUseCase getReceiptsPagedUseCase() {
        return getReceiptsPagedUseCase;
    }

    GetAllItemsPagedUseCase getAllItemsPagedUseCase() {
        return getAllItemsPagedUseCase;
    }

    GetReceiptByIdUseCase getReceiptByIdUseCase() {
        return getReceiptByIdUseCase;
    }

    DeleteReceiptUseCase deleteReceiptUseCase() {
        return deleteReceiptUseCase;
    }
}
