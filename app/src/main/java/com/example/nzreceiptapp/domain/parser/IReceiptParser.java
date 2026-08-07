package com.example.nzreceiptapp.domain.parser;

import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import java.util.List;

/**
 * 各超市解析器的統一介面 (Domain Layer)
 */
public interface IReceiptParser {
    /**
     * 將 OCR 文字解析為商品明細列表
     */
    List<ReceiptItem> parseRawText(String rawText);

    /**
     * Rich parser result used by the receipt workflow. Existing parser tests can
     * continue exercising parseRawText directly.
     */
    default ParsedReceipt parseReceipt(String rawText) {
        return new ParsedReceipt(parseRawText(rawText), null);
    }
}
