package com.example.nzreceiptapp.di;

import android.content.Context;

import com.example.nzreceiptapp.data.image.OpenCvReceiptImagePreprocessor;
import com.example.nzreceiptapp.data.local.AppDatabase;
import com.example.nzreceiptapp.data.local.BundledCategoryInitializer;
import com.example.nzreceiptapp.data.local.LocalReceiptImageStore;
import com.example.nzreceiptapp.data.ocr.MLKitOCRService;
import com.example.nzreceiptapp.data.parser.ParserProvider;
import com.example.nzreceiptapp.data.repository.CategoryRepositoryImpl;
import com.example.nzreceiptapp.data.repository.ReceiptRepositoryImpl;
import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.repository.ICategoryRepository;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;
import com.example.nzreceiptapp.domain.service.IOCRService;
import com.example.nzreceiptapp.domain.service.IReceiptImagePreprocessor;
import com.example.nzreceiptapp.domain.service.IReceiptImageStore;
import com.example.nzreceiptapp.domain.usecase.DeleteReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAllItemsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.GetCategoriesUseCase;
import com.example.nzreceiptapp.domain.usecase.GetAnalyticsUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptByIdUseCase;
import com.example.nzreceiptapp.domain.usecase.GetReceiptsPagedUseCase;
import com.example.nzreceiptapp.domain.usecase.ParseReceiptUseCase;
import com.example.nzreceiptapp.domain.usecase.SaveReceiptUseCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Composition root for the app. */
public final class AppContainer {
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);

    private final IOCRService ocrService;
    private final ParseReceiptUseCase parseReceiptUseCase;
    private final SaveReceiptUseCase saveReceiptUseCase;
    private final GetReceiptsPagedUseCase getReceiptsPagedUseCase;
    private final GetAllItemsPagedUseCase getAllItemsPagedUseCase;
    private final GetReceiptByIdUseCase getReceiptByIdUseCase;
    private final DeleteReceiptUseCase deleteReceiptUseCase;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final GetAnalyticsUseCase getAnalyticsUseCase;
    private final IReceiptImageStore imageStore;
    private final IReceiptImagePreprocessor imagePreprocessor;

    public AppContainer(Context context) {
        Context applicationContext = context.getApplicationContext();
        AppDatabase database = AppDatabase.getDatabase(applicationContext);

        ICategoryRepository categoryRepository =
                new CategoryRepositoryImpl(database.categoryDao());
        imageStore = new LocalReceiptImageStore(applicationContext);
        imagePreprocessor = new OpenCvReceiptImagePreprocessor(applicationContext);

        IReceiptRepository receiptRepository =
                new ReceiptRepositoryImpl(database.receiptDao(), imageStore);

        CategoryClassifier classifier = new CategoryClassifier(categoryRepository);
        ParserProvider parserProvider = new ParserProvider();
        BundledCategoryInitializer categoryInitializer =
                new BundledCategoryInitializer(applicationContext, categoryRepository);

        ocrService = new MLKitOCRService(applicationContext);
        parseReceiptUseCase = new ParseReceiptUseCase(
                parserProvider, categoryInitializer, classifier);
        saveReceiptUseCase = new SaveReceiptUseCase(receiptRepository);
        getReceiptsPagedUseCase = new GetReceiptsPagedUseCase(receiptRepository);
        getAllItemsPagedUseCase = new GetAllItemsPagedUseCase(receiptRepository);
        getReceiptByIdUseCase = new GetReceiptByIdUseCase(receiptRepository);
        deleteReceiptUseCase = new DeleteReceiptUseCase(receiptRepository);
        getCategoriesUseCase = new GetCategoriesUseCase(categoryRepository);
        getAnalyticsUseCase = new GetAnalyticsUseCase(receiptRepository);
    }

    ExecutorService ioExecutor() { return ioExecutor; }
    IOCRService ocrService() { return ocrService; }
    ParseReceiptUseCase parseReceiptUseCase() { return parseReceiptUseCase; }
    SaveReceiptUseCase saveReceiptUseCase() { return saveReceiptUseCase; }
    GetReceiptsPagedUseCase getReceiptsPagedUseCase() { return getReceiptsPagedUseCase; }
    GetAllItemsPagedUseCase getAllItemsPagedUseCase() { return getAllItemsPagedUseCase; }
    GetReceiptByIdUseCase getReceiptByIdUseCase() { return getReceiptByIdUseCase; }
    DeleteReceiptUseCase deleteReceiptUseCase() { return deleteReceiptUseCase; }
    GetCategoriesUseCase getCategoriesUseCase() { return getCategoriesUseCase; }
    GetAnalyticsUseCase getAnalyticsUseCase() { return getAnalyticsUseCase; }
    IReceiptImageStore imageStore() { return imageStore; }
    IReceiptImagePreprocessor imagePreprocessor() { return imagePreprocessor; }
}
