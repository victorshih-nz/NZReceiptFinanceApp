package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 專門解析紐西蘭 PAK'nSAVE 收據文字的解析器 (Data Layer)
 */
public class PakNSaveParser implements IReceiptParser {

    @Override
    public List<ReceiptItem> parseRawText(String ocrText) {
        List<ReceiptItem> items = new ArrayList<>();
        if (ocrText == null || ocrText.trim().isEmpty()) {
            return items;
        }

        String[] lines = ocrText.split("\n");

        Pattern datePattern = Pattern.compile("(\\d{2})[./](\\d{2})[./](\\d{2})");
        Pattern totalPattern = Pattern.compile("TOTAL(?:\\s+DUE)?\\s+\\$?\\s*(\\d+\\.\\d{2})", Pattern.CASE_INSENSITIVE);
        Pattern itemPattern = Pattern.compile("^(.+?)\\s+\\$?\\s*(\\d+\\.\\d{2})$");
        Pattern excludePattern = Pattern.compile("(EFTPOS|CASH|CHANGE|ROUNDING|GST|TOTAL|SUBTOTAL)", Pattern.CASE_INSENSITIVE);
        Pattern weightInfoPattern = Pattern.compile("^([\\d.]+)\\s*([a-zA-Z]+)\\s*@\\s*\\$?\\s*([\\d.]+)");
        
        String pendingItemName = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // ========================================================
            // 2. 解析收據總金額 (此處僅作過濾或記錄，目前回傳 List)
            // ========================================================
            Matcher totalMatcher = totalPattern.matcher(line);
            if (totalMatcher.find()) {
                continue;
            }

            // 3. 解析商品明細
            Matcher itemMatcher = itemPattern.matcher(line);
            if (itemMatcher.matches()) {
                String matchedPart = itemMatcher.group(1).trim();
                double matchedPrice = Double.parseDouble(itemMatcher.group(2));

                if (excludePattern.matcher(matchedPart).find()) {
                    continue;
                }

                String itemId = UUID.randomUUID().toString();

                // 狀況 A：如果是「秤重/多件商品」的第二行說明 (包含 @)
                if (matchedPart.contains("@")) {
                    Matcher weightMatcher = weightInfoPattern.matcher(matchedPart);

                    double quantity = 1.0;
                    String unit = "kg";
                    long unitPriceCents = Math.round(matchedPrice * 100);

                    if (weightMatcher.find()) {
                        quantity = Double.parseDouble(weightMatcher.group(1));
                        unit = weightMatcher.group(2);
                        double unitPrice = Double.parseDouble(weightMatcher.group(3));
                        unitPriceCents = Math.round(unitPrice * 100);
                    }

                    String finalName = (pendingItemName != null) ? pendingItemName : matchedPart;

                    items.add(new ReceiptItem(
                            itemId, matchedPart, finalName, quantity, unit, unitPriceCents, Collections.emptyList(), null, false
                    ));

                    pendingItemName = null;
                    continue;
                }

                // 狀況 B：【正常品項】
                long unitPriceCents = Math.round(matchedPrice * 100);

                items.add(new ReceiptItem(
                        itemId, matchedPart, matchedPart, 1.0, "ea", unitPriceCents, Collections.emptyList(), null, false
                ));

                pendingItemName = null;

            } else {
                if (!excludePattern.matcher(line).find() && line.length() > 3) {
                    pendingItemName = line;
                }
            }
        }

        return items;
    }
}
