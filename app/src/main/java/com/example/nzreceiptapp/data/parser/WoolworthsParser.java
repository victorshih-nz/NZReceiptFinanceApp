package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.model.ReceiptItem;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WoolworthsParser {

    // 第一層：擷取品名與總價，捕捉前綴 ^, *, #, 忽略稅務代碼 G, N, *
    private static final Pattern ITEM_PATTERN = Pattern.compile("^([\\^\\*#]?)\\s*(.+?)\\s+(\\d+\\.\\d+)\\s*(?:[GN*])?\\s*$");

    // 多件模式：支援 "2 @ 1.50", "Qty 2 @ 1.50 each", "$1.50 each"
    private static final Pattern MULTI_BUY_INFO = Pattern.compile("(?:Qty\\s*)?(\\d+(?:\\.\\d+)?)\\s*@\\s*\\$?(\\d+\\.\\d+)(?:\\s*each)?");

    // 秤重模式：支援 "0 520 kg NET @ $1.95/kg"
    private static final Pattern WEIGHTED_INFO = Pattern.compile("(\\d+[\\s.]\\d+)\\s*kg\\s*(?:NET\\s*)?@\\s*\\$?(\\d+\\.\\d+)\\s*/kg");

    // 純多件折落模式 (不含總價在同行的狀況)
    private static final Pattern MULTI_BUY_ONLY = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*@\\s*\\$?(\\d+\\.\\d+)\\s*$");

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
                continue;
            }

            // 2. 處理「純多件折落行」(上一行的延續，無總價在同行)
            Matcher multiOnlyMatcher = MULTI_BUY_ONLY.matcher(line);
            if (multiOnlyMatcher.matches()) {
                if (!items.isEmpty()) {
                    ReceiptItem lastItem = items.get(items.size() - 1);
                    lastItem.setQuantity(Double.parseDouble(multiOnlyMatcher.group(1)));
                    lastItem.setUnitPrice(Double.parseDouble(multiOnlyMatcher.group(2)));
                }
                continue;
            }

            // 3. 標準品項 (品名 + 總價)
            Matcher itemMatcher = ITEM_PATTERN.matcher(trimmedLine);
            if (itemMatcher.matches()) {
                String prefix = itemMatcher.group(1);
                String rawName = itemMatcher.group(2).trim();
                double totalPrice = Double.parseDouble(itemMatcher.group(3));

                ReceiptItem item = new ReceiptItem();
                item.setTotalPrice(totalPrice);
                item.setQuantity(1.0);
                item.setUnitPrice(totalPrice);
                
                // 判斷是否為特價品 (^)
                if ("^".equals(prefix) || pendingIsSpecial) {
                    item.setSpecialMk(true);
                }

                // 檢查是否為秤重資訊
                Matcher weightMatcher = WEIGHTED_INFO.matcher(rawName);
                if (weightMatcher.find()) {
                    String qtyStr = weightMatcher.group(1).replace(" ", ".");
                    item.setQuantity(Double.parseDouble(qtyStr));
                    item.setUnitPrice(Double.parseDouble(weightMatcher.group(2)));
                    item.setUnit("kg");
                    
                    if (pendingItemName != null) {
                        item.setName(pendingItemName);
                    } else {
                        String namePrefix = rawName.substring(0, weightMatcher.start()).trim();
                        item.setName(namePrefix.isEmpty() ? "Unknown Weighted" : namePrefix);
                    }
                } 
                // 檢查是否為多件資訊
                else {
                    Matcher multiInfoMatcher = MULTI_BUY_INFO.matcher(rawName);
                    if (multiInfoMatcher.find()) {
                        item.setQuantity(Double.parseDouble(multiInfoMatcher.group(1)));
                        item.setUnitPrice(Double.parseDouble(multiInfoMatcher.group(2)));
                        
                        if (pendingItemName != null) {
                            item.setName(pendingItemName);
                        } else {
                            String namePrefix = rawName.substring(0, multiInfoMatcher.start()).trim();
                            item.setName(namePrefix.isEmpty() ? "Unknown Multi" : namePrefix);
                        }
                    } else {
                        item.setName(rawName);
                    }
                }

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
                upper.contains("PH:");
    }
}
