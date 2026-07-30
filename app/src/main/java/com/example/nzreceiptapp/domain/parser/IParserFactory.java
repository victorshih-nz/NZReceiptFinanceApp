package com.example.nzreceiptapp.domain.parser;

/**
 * 解析器工廠介面 (Domain Layer)
 */
public interface IParserFactory {
    IReceiptParser getParser(String chainName);
}
