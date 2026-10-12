package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.parser.IParserFactory;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;

/** 提供對應超市解析器的工廠類 (Data Layer) */
public class ParserProvider implements IParserFactory {
    private final IReceiptParser woolworthsParser = new WoolworthsParser();
    private final IReceiptParser pakNSaveParser = new PakNSaveParser();
    private final IReceiptParser kmartParser = new KmartParser();
    private final IReceiptParser taiPingParser = new TaiPingParser();
    private final IReceiptParser ikeaParser = new IkeaParser();
    private final IReceiptParser chemistWarehouseParser = new ChemistWarehouseParser();

    @Override public IReceiptParser getParser(String chainName) {
        if (chainName == null) return null;
        String normalized = normalize(chainName);
        if (normalized.contains("woolworths") || normalized.contains("countdown")) return woolworthsParser;
        if (normalized.contains("paknsave")) return pakNSaveParser;
        if (normalized.contains("kmart")) return kmartParser;
        if (normalized.contains("taiping")) return taiPingParser;
        if (normalized.contains("ikea")) return ikeaParser;
        if (normalized.contains("chemistwarehouse")) return chemistWarehouseParser;
        return null;
    }

    @Override public String detectChain(String rawText) {
        if (rawText == null) return null;
        String normalized = normalize(rawText);
        if (normalized.contains("woolworths") || normalized.contains("countdown")) return "Woolworths";
        if (normalized.contains("paknsave")) return "PAK'nSAVE";
        if (normalized.contains("kmart")) return "Kmart";
        if (normalized.contains("taipingasiansupermarket") || normalized.contains("taiping")) return "Tai Ping";
        if (normalized.contains("ikeanewzealand") || normalized.contains("ikea")) return "IKEA";
        if (normalized.contains("chemistwarehouse")) return "Chemist Warehouse";
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replace("'", "").replaceAll("[^a-z0-9]+", "");
    }
}
