package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 將 OCR 文字解析為 Receipt 領域物件的 Use Case
 */
public class ParseReceiptUseCase {

    private final IParserFactory parserFactory;

    public ParseReceiptUseCase(IParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

    public Receipt execute(String rawText, String chainName, String branchName, LocalDateTime purchaseDate) {
        IReceiptParser parser = parserFactory.getParser(chainName);
        if (parser == null) {
            throw new IllegalArgumentException("Unsupported supermarket chain: " + chainName);
        }

        List<ReceiptItem> items = parser.parseRawText(rawText);
        
        Store store = new Store(UUID.randomUUID().toString(), chainName, branchName);
        
        return new Receipt(
                UUID.randomUUID().toString(),
                store,
                items,
                purchaseDate != null ? purchaseDate : LocalDateTime.now(),
                0, // 初始折扣設為 0，可由使用者後續調整
                false
        );
    }
}
