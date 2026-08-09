package com.example.nzreceiptapp.domain.parser;

import com.example.nzreceiptapp.domain.model.ParsedReceipt;

/**
 * 各超市解析器的統一介面 (Domain Layer)
 */
public interface IReceiptParser {
    /** Parses chain-specific OCR text into items and receipt-level metadata. */
    ParsedReceipt parse(String rawText);
}
