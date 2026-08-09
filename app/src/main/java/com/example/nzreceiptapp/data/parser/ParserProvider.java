package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;

/**
 * 提供對應超市解析器的工廠類 (Data Layer)
 */
public class ParserProvider implements IParserFactory {

    private final IReceiptParser woolworthsParser = new WoolworthsParser();
    private final IReceiptParser pakNSaveParser = new PakNSaveParser();

    /**
     * 根據連鎖店名稱獲取解析器
     */
    @Override
    public IReceiptParser getParser(String chainName) {
        if (chainName == null) return null;

        String normalized = chainName.toLowerCase().replace(" ", "").replace("'", "");
        
        if (normalized.contains("woolworths") || normalized.contains("countdown")) {
            return woolworthsParser;
        } else if (normalized.contains("paknsave")) {
            return pakNSaveParser;
        }
        
        // The use case turns an unsupported chain into a user-facing error.
        return null;
    }

    @Override
    public String detectChain(String rawText) {
        if (rawText == null) return null;
        String normalized = rawText.toLowerCase().replace("'", "").replace(" ", "");
        if (normalized.contains("woolworths")) {
            return "Woolworths";
        }
        if (normalized.contains("paknsave")) {
            return "PAK'nSAVE";
        }
        return null;
    }
}
