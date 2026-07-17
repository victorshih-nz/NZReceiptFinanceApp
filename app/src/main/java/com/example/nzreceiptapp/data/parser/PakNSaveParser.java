package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PakNSaveParser {

    public List<ReceiptItem> parseRawText(String ocrText) {
        Receipt receipt = new Receipt();
        receipt.setStoreName("PAK'nSAVE");

        List<ReceiptItem> items = new ArrayList<>();
        String[] lines = ocrText.split("\n");

        // 原程式碼第 21 行附近
        Pattern datePattern = Pattern.compile("(\\d{2})[./](\\d{2})[./](\\d{2})");
        Pattern totalPattern = Pattern.compile("TOTAL(?:\\s+DUE)?\\s+\\$?\\s*(\\d+\\.\\d{2})", Pattern.CASE_INSENSITIVE);
        Pattern itemPattern = Pattern.compile("^(.+?)\\s+\\$?\\s*(\\d+\\.\\d{2})$");
        Pattern excludePattern = Pattern.compile("(EFTPOS|CASH|CHANGE|ROUNDING|GST|TOTAL|SUBTOTAL)", Pattern.CASE_INSENSITIVE);

        //  修正後的 weightInfoPattern
        Pattern weightInfoPattern = Pattern.compile("^([\\d.]+)\\s*([a-zA-Z]+)\\s*@\\s*\\$?\\s*([\\d.]+)");
        String pendingItemName = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 1. 解析交易日期
            if (receipt.getTransactionDate() == null) {
                Matcher dateMatcher = datePattern.matcher(line);
                if (dateMatcher.find()) {
                    receipt.setTransactionDate(dateMatcher.group(0));
                }
            }

            // ========================================================
            // 2. 解析收據總金額 (修正：將 double 轉為 long Cents)
            // ========================================================
            Matcher totalMatcher = totalPattern.matcher(line);
            if (totalMatcher.find()) {
                double totalDouble = Double.parseDouble(totalMatcher.group(1));
                long totalCents = Math.round(totalDouble * 100); // 例如 14.24 -> 1424L
                receipt.setTotalAmountCents(totalCents);
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
                            itemId, finalName, finalName, quantity, unit, unitPriceCents, 0L
                    ));

                    pendingItemName = null;
                    continue;
                }

                // 狀況 B：【正常品項】的 Assign 方式
                long unitPriceCents = Math.round(matchedPrice * 100);

                items.add(new ReceiptItem(
                        itemId, matchedPart, matchedPart, 1.0, "ea", unitPriceCents, 0L
                ));

                pendingItemName = null;

            } else {
                if (!excludePattern.matcher(line).find() && line.length() > 3) {
                    pendingItemName = line;
                }
            }
        }

       // receipt.setItems(items);
        return items;
    }
}