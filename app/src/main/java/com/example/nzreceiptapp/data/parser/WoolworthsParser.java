package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.model.ReceiptItem;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WoolworthsParser {

    // 1. 標準品項行：匹配名稱與總價 (修正：相容選填的 $ 以及行尾的 GST 稅收標記如 G, N, *)
    private static final Pattern ITEM_PATTERN =
            Pattern.compile("^(.+?)\\s+\\$?\\s*(\\d+\\.\\d{2})(?:\\s+[A-Z*])?\\s*$", Pattern.CASE_INSENSITIVE);

    // 2. 獨立明細行：例如 "2 @ 1.50" 或 "0.645 kg @ $3.49 /kg" (修正：不強制行尾有總價，相容稅率標記)
    private static final Pattern STANDALONE_MULTI_PATTERN =
            Pattern.compile("^\\s*([\\d.]+)\\s*([a-zA-Z]*)\\s*@\\s*\\$?\\s*([\\d.]+)(?:\\s*ea|\\s*/?kg)?(?:\\s+[A-Z*])?\\s*$", Pattern.CASE_INSENSITIVE);

    // 3. 內嵌明細解析：用於拆解同一行內的 @ 資訊
    private static final Pattern INNER_DETAIL_PATTERN =
            Pattern.compile("([\\d.]+)\\s*([a-zA-Z]*)\\s*@\\s*\\$?\\s*([\\d.]+)(?:\\s*ea|\\s*/?kg)?", Pattern.CASE_INSENSITIVE);

    // 4. 過濾無效關鍵字
    private static final Pattern EXCLUDE_PATTERN =
            Pattern.compile("(EFTPOS|CASH|CHANGE|ROUNDING|GST|TOTAL|SUBTOTAL|BAL|ITEMS|TAX INVOICE|DUPLICATE)", Pattern.CASE_INSENSITIVE);

    public List<ReceiptItem> parseRawText(String ocrText) {
        List<ReceiptItem> items = new ArrayList<>();
        String[] lines = ocrText.split("\n");
        String pendingItemName = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 優先情境 A：此行為獨立的 @ 數量金額拆解行 (例如上一行是品項，這行是 2 @ 1.50)
            Matcher standaloneMatcher = STANDALONE_MULTI_PATTERN.matcher(line);
            if (standaloneMatcher.matches()) {
                if (!items.isEmpty()) {
                    // 核心修正：回溯並更新剛剛加入的上一個商品的數量與單價
                    double quantity = Double.parseDouble(standaloneMatcher.group(1));
                    String rawUnit = standaloneMatcher.group(2).trim();
                    double unitPrice = Double.parseDouble(standaloneMatcher.group(3));

                    ReceiptItem lastItem = items.get(items.size() - 1);
                    lastItem.setQuantity(quantity);
                    lastItem.setUnit(rawUnit.isEmpty() ? "ea" : rawUnit);
                    lastItem.setUnitPriceCents(Math.round(unitPrice * 100));
                }
                pendingItemName = null;
                continue;
            }

            // 情境 B：標準商品行 (包含品項名稱/敘述與該行總價)
            Matcher itemMatcher = ITEM_PATTERN.matcher(line);
            if (itemMatcher.matches()) {
                String matchedPart = itemMatcher.group(1).trim();
                double matchedPrice = Double.parseDouble(itemMatcher.group(2));

                if (EXCLUDE_PATTERN.matcher(matchedPart).find()) {
                    continue;
                }

                String itemId = UUID.randomUUID().toString();
                long totalCents = Math.round(matchedPrice * 100);

                // 子情境 B-1：同一行內包含 @ 符號 (例如：BANANAS LOOSE 0.645 kg @ $3.49 /kg)
                if (matchedPart.contains("@")) {
                    Matcher innerMatcher = INNER_DETAIL_PATTERN.matcher(matchedPart);

                    double quantity = 1.0;
                    String unit = "ea";
                    long unitPriceCents = totalCents;

                    if (innerMatcher.find()) {
                        quantity = Double.parseDouble(innerMatcher.group(1));
                        String rawUnit = innerMatcher.group(2).trim();
                        double unitPrice = Double.parseDouble(innerMatcher.group(3));

                        unit = rawUnit.isEmpty() ? "ea" : rawUnit;
                        unitPriceCents = Math.round(unitPrice * 100);
                    }

                    // 切除字串內部的 @ 資訊，只留下乾淨的商品名稱
                    String cleanName = matchedPart.split("(?=\\d+\\s*([a-zA-Z]*)\\s*@)")[0].trim();
                    if (cleanName.isEmpty()) {
                        cleanName = (pendingItemName != null) ? pendingItemName : matchedPart;
                    }

                    items.add(new ReceiptItem(itemId, cleanName, matchedPart, quantity, unit, unitPriceCents, totalCents));
                    pendingItemName = null;
                    continue;
                }

                // 子情境 B-2：這一行的前半段是 @ 明細，但商品名稱在上一行 (例如 Line1: MILK, Line2: 2 @ $1.50 ea 3.00 G)
                if (matchedPart.matches(".*\\d+\\s*([a-zA-Z]*)\\s*@.*")) {
                    Matcher innerMatcher = INNER_DETAIL_PATTERN.matcher(matchedPart);
                    double quantity = 1.0;
                    String unit = "ea";
                    long unitPriceCents = totalCents;

                    if (innerMatcher.find()) {
                        quantity = Double.parseDouble(innerMatcher.group(1));
                        String rawUnit = innerMatcher.group(2).trim();
                        double unitPrice = Double.parseDouble(innerMatcher.group(3));
                        unit = rawUnit.isEmpty() ? "ea" : rawUnit;
                        unitPriceCents = Math.round(unitPrice * 100);
                    }

                    String finalName = (pendingItemName != null) ? pendingItemName : matchedPart;
                    items.add(new ReceiptItem(itemId, finalName, line, quantity, unit, unitPriceCents, totalCents));
                    pendingItemName = null;
                    continue;
                }

                // 子情境 B-3：標準單件商品 (數量預設 1)
                String finalName = (pendingItemName != null) ? pendingItemName : matchedPart;
                items.add(new ReceiptItem(itemId, finalName, matchedPart, 1.0, "ea", totalCents, totalCents));
                pendingItemName = null;

            } else {
                // 情境 C：折行或純商品名稱行，先暫存起來供下一行結合
                if (!EXCLUDE_PATTERN.matcher(line).find() && line.length() > 2) {
                    pendingItemName = line;
                }
            }
        }
        return items;
    }
}