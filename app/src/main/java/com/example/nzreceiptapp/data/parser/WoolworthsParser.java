package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.logic.CategoryClassifier;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.ItemDiscount;
import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 專門解析紐西蘭 Woolworths 收據文字的解析器 (Data Layer)
 */
public class WoolworthsParser implements IReceiptParser {

    private final CategoryClassifier classifier;

    public WoolworthsParser() {
        this.classifier = null;
    }

    public WoolworthsParser(CategoryClassifier classifier) {
        this.classifier = classifier;
    }

    // 第一層：擷取品名與總價，捕捉前綴 ^, *, #，忽略稅務代碼 G, N, *
    private static final Pattern ITEM_PATTERN = Pattern.compile("^([\\^\\*#]?)\\s*(.+?)\\s+(\\d+\\.\\d+)\\s*(?:[GN*])?\\s*$");

    // 多件模式：支援 "2 @ 1.50", "Qty 2 @ 1.50 each", "$1.50 each"
    private static final Pattern MULTI_BUY_INFO = Pattern.compile("(?:Qty\\s*)?(\\d+(?:\\.\\d+)?)\\s*@\\s*\\$?(\\d+\\.\\d+)(?:\\s*each)?");

    // 秤重模式：支援 "0 520 kg NET @ $1.95/kg"
    private static final Pattern WEIGHTED_INFO = Pattern.compile("(\\d+[\\s.]\\d+)\\s*kg\\s*(?:NET\\s*)?@\\s*\\$?(\\d+\\.\\d+)\\s*/kg");

    // 純多件折落模式 (不含總價在同行的狀況)
    private static final Pattern MULTI_BUY_ONLY = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*@\\s*\\$?(\\d+\\.\\d+)\\s*$");

    private static final Pattern PRINTED_TOTAL_PATTERN = Pattern.compile(
            "(?im)^\\s*TOTAL(?:\\s+DUE)?\\s+\\$?\\s*(\\d+\\.\\d{2})\\s*$");

    @Override
    public List<ReceiptItem> parseRawText(String rawText) {
        List<ReceiptItem> items = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            return items;
        }

        String pendingItemName = null;
        boolean pendingIsSpecial = false;
        String[] lines = rawText.split("\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // 1. 關鍵字過濾
            if (isTransactionMetadata(trimmedLine)) {
                // 如果是 metadata，應該清空 pendingItemName，避免誤用到下一個品項
                pendingItemName = null;
                continue;
            }

            // 2. 處理「純多件折落行」(上一行的延續，無總價在同行)
            Matcher multiOnlyMatcher = MULTI_BUY_ONLY.matcher(line);
            if (multiOnlyMatcher.matches()) {
                if (!items.isEmpty()) {
                    ReceiptItem lastItem = items.remove(items.size() - 1);
                    double qty = Double.parseDouble(multiOnlyMatcher.group(1));
                    long unitPriceCents = Math.round(Double.parseDouble(multiOnlyMatcher.group(2)) * 100);
                    
                    ReceiptItem updatedItem = new ReceiptItem(
                        lastItem.getId(),
                        lastItem.getRawName(),
                        lastItem.getCleanedName(),
                        qty,
                        lastItem.getUnit(),
                        unitPriceCents,
                        lastItem.getDiscounts(),
                        lastItem.getCategory(),
                        lastItem.getSpecialMk()
                    );
                    items.add(updatedItem);
                }
                continue;
            }

            // 3. 標準品項 (品名 + 總價)
            Matcher itemMatcher = ITEM_PATTERN.matcher(trimmedLine);
            if (itemMatcher.matches()) {
                String prefix = itemMatcher.group(1);
                String rawNameFromMatch = itemMatcher.group(2).trim();
                double totalPrice = Double.parseDouble(itemMatcher.group(3));

                String name = rawNameFromMatch;
                double quantity = 1.0;
                String unit = "ea";
                long unitPriceCents = Math.round(totalPrice * 100);
                boolean isSpecial = "^".equals(prefix) || pendingIsSpecial;

                // 檢查是否為秤重資訊
                Matcher weightMatcher = WEIGHTED_INFO.matcher(rawNameFromMatch);
                if (weightMatcher.find()) {
                    String qtyStr = weightMatcher.group(1).replace(" ", ".");
                    quantity = Double.parseDouble(qtyStr);
                    unitPriceCents = Math.round(Double.parseDouble(weightMatcher.group(2)) * 100);
                    unit = "kg";
                    
                    if (pendingItemName != null) {
                        name = pendingItemName;
                    } else {
                        String namePrefix = rawNameFromMatch.substring(0, weightMatcher.start()).trim();
                        name = namePrefix.isEmpty() ? "Unknown Weighted" : namePrefix;
                    }
                } 
                // 檢查是否為多件資訊
                else {
                    Matcher multiInfoMatcher = MULTI_BUY_INFO.matcher(rawNameFromMatch);
                    if (multiInfoMatcher.find()) {
                        quantity = Double.parseDouble(multiInfoMatcher.group(1));
                        unitPriceCents = Math.round(Double.parseDouble(multiInfoMatcher.group(2)) * 100);
                        
                        if (pendingItemName != null) {
                            name = pendingItemName;
                        } else {
                            String namePrefix = rawNameFromMatch.substring(0, multiInfoMatcher.start()).trim();
                            name = namePrefix.isEmpty() ? "Unknown Multi" : namePrefix;
                        }
                    } else {
                        // 一般品項，直接使用 rawNameFromMatch，除非它非常短且有 pendingItemName
                        if (pendingItemName != null && rawNameFromMatch.length() < 3) {
                             name = pendingItemName + " " + rawNameFromMatch;
                        } else {
                             name = rawNameFromMatch;
                        }
                    }
                }

                ReceiptItem item = new ReceiptItem(
                    UUID.randomUUID().toString(),
                    rawNameFromMatch,
                    name,
                    quantity,
                    unit,
                    unitPriceCents,
                    Collections.emptyList(),
                    classifier != null ? classifier.classify(name) : null,
                    isSpecial
                );

                items.add(item);
                pendingItemName = null;
                pendingIsSpecial = false;
            } else {
                // 如果沒有總價，可能是品名行
                if (trimmedLine.length() > 2) {
                    pendingIsSpecial = trimmedLine.startsWith("^");
                    pendingItemName = trimmedLine.replaceAll("^[\\^\\*#]\\s*", "").trim();
                }
            }
        }

        return items;
    }

    @Override
    public ParsedReceipt parseReceipt(String rawText) {
        List<ReceiptItem> items = parseRawText(rawText);
        Matcher matcher = PRINTED_TOTAL_PATTERN.matcher(rawText == null ? "" : rawText);
        Long total = matcher.find()
                ? Math.round(Double.parseDouble(matcher.group(1)) * 100)
                : null;
        return new ParsedReceipt(items, total);
    }

    private boolean isTransactionMetadata(String line) {
        String upper = line.toUpperCase();
        return upper.startsWith("TOTAL") ||
                upper.contains("SUBTOTAL") ||
                upper.startsWith("EFTPOS") ||
                upper.startsWith("CASH") ||
                upper.startsWith("STORE ") ||
                upper.contains("TAX INVOICE") ||
                upper.contains("WELCOME") ||
                upper.contains("THANK YOU") ||
                upper.contains("DUPLICATE") ||
                upper.contains("GREENLANE") ||
                upper.contains("GREAT SOUTH ROAD") ||
                upper.contains("VISA DEBIT") ||
                upper.contains("NZ$") ||
                upper.contains("TERM ID") ||
                upper.contains("PH:") ||
                upper.contains("WOOLWORTHS") ||
                upper.contains("PAK'NSAVE");
    }
}
