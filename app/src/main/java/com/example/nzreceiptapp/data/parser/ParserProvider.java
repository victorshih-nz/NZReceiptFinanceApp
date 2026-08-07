package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;

/**
 * 提供對應超市解析器的工廠類 (Data Layer)
 */
public class ParserProvider implements IParserFactory {

    private final CategoryClassifier classifier;

    public ParserProvider(CategoryClassifier classifier) {
        this.classifier = classifier;
    }

    /**
     * 根據連鎖店名稱獲取解析器
     */
    @Override
    public IReceiptParser getParser(String chainName) {
        if (chainName == null) return null;

        String normalized = chainName.toLowerCase().replace(" ", "").replace("'", "");
        
        if (normalized.contains("woolworths") || normalized.contains("countdown")) {
            return new WoolworthsParser(classifier);
        } else if (normalized.contains("paknsave")) {
            return new PakNSaveParser(classifier);
        }
        
        // 預設返回 null 或一個基礎解析器
        return null;
    }

    @Override
    public String detectChain(String rawText) {
        if (rawText == null) return null;
        String normalized = rawText.toLowerCase().replace("'", "").replace(" ", "");
        if (normalized.contains("woolworths") || normalized.contains("countdown")) {
            return "Woolworths";
        }
        if (normalized.contains("paknsave")) {
            return "PAK'nSAVE";
        }
        return null;
    }
}
