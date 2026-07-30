package com.example.nzreceiptapp.domain.parser;

import com.example.nzreceiptapp.domain.model.ReceiptItem;
import java.util.List;

/**
 * 各超市解析器的統一介面 (Domain Layer)
 */
public interface IReceiptParser {
    /**
     * 將 OCR 文字解析為商品明細列表
     */
    List<ReceiptItem> parseRawText(String rawText);
}
