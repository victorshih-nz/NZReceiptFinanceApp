package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;
import com.example.nzreceiptapp.domain.service.ICategoryInitializer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 將 OCR 文字解析為 Receipt 領域物件的 Use Case
 */
public class ParseReceiptUseCase {

    private final IParserFactory parserFactory;
    private final ICategoryInitializer categoryInitializer;
    private final CategoryClassifier categoryClassifier;

    public ParseReceiptUseCase(IParserFactory parserFactory,
                               ICategoryInitializer categoryInitializer,
                               CategoryClassifier categoryClassifier) {
        this.parserFactory = parserFactory;
        this.categoryInitializer = categoryInitializer;
        this.categoryClassifier = categoryClassifier;
    }

    public Receipt execute(String rawText, String chainName, String branchName, LocalDateTime purchaseDate) {
        return execute(rawText, chainName, branchName, purchaseDate, null);
    }

    public Receipt execute(String rawText, String chainName, String branchName,
                           LocalDateTime purchaseDate, String imageUri) {
        categoryInitializer.ensureInitialized();

        String resolvedChain = chainName;
        if (resolvedChain == null || resolvedChain.trim().isEmpty()
                || "Auto detect".equalsIgnoreCase(resolvedChain.trim())) {
            resolvedChain = parserFactory.detectChain(rawText);
        }
        if (resolvedChain == null) {
            throw new IllegalArgumentException(
                    "Could not detect the supermarket. Please choose one before scanning.");
        }

        IReceiptParser parser = parserFactory.getParser(resolvedChain);
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported supermarket chain: " + resolvedChain);
        }

        ParsedReceipt parsed = parser.parse(rawText);
        List<ReceiptItem> classifiedItems = classifyItems(parsed.getItems());
        if (classifiedItems.isEmpty()) {
            throw new IllegalArgumentException("No receipt items could be recognised");
        }
        
        String resolvedBranch = branchName == null ? "" : branchName.trim();
        Store store = new Store(UUID.randomUUID().toString(), resolvedChain, resolvedBranch);
        
        return new Receipt(
                UUID.randomUUID().toString(),
                store,
                classifiedItems,
                purchaseDate != null ? purchaseDate : LocalDateTime.now(),
                0, // 初始折扣設為 0，可由使用者後續調整
                false,
                rawText,
                imageUri,
                parsed.getPrintedTotalCents()
        );
    }

    private List<ReceiptItem> classifyItems(List<ReceiptItem> parsedItems) {
        List<ReceiptItem> result = new ArrayList<>();
        for (ReceiptItem item : parsedItems) {
            result.add(item.withCategory(
                    categoryClassifier.classify(item.getCleanedName())));
        }
        return result;
    }
}
