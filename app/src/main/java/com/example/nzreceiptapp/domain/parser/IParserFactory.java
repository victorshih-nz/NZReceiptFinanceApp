package com.example.nzreceiptapp.domain.parser;

/**
 * 解析器工廠介面 (Domain Layer)
 */
public interface IParserFactory {
    IReceiptParser getParser(String chainName);

    /** Returns a canonical chain name, or null when the receipt is unknown. */
    default String detectChain(String rawText) {
        return null;
    }
}
