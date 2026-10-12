package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chemist Warehouse NZ: inline, split-price, and guarded single-item OCR fallback. */
public class ChemistWarehouseParser implements IReceiptParser {
    private static final Pattern INLINE_ITEM = Pattern.compile(
            "^\\s*(\\d+)\\s+(.+?)\\s+\\$?\\s*(\\d+[.,]\\d{2})\\s*\\*?\\s*$");
    private static final Pattern ITEM_WITHOUT_PRICE = Pattern.compile(
            "^\\s*(\\d+)\\s+(.+?\\S)\\s*$");
    private static final Pattern MONEY_ONLY = Pattern.compile(
            "^\\s*\\$?\\s*(\\d+[.,]\\d{2})\\s*\\*?\\s*$");
    private static final Pattern TOTAL = Pattern.compile(
            "(?im)^\\s*TOTAL\\s+\\$?\\s*(\\d+[.,]\\d{2})(?:\\s|$).*$");
    private static final Pattern SINGLE_ITEM_COUNT = Pattern.compile(
            "(?im)^\\s*1\\s+ITEM\\(S\\)\\s*$");

    @Override
    public ParsedReceipt parse(String rawText) {
        List<ReceiptItem> items = new ArrayList<>();
        Integer pendingQuantity = null;
        String pendingName = null;
        int unpricedCandidates = 0;
        boolean insideItems = false;
        boolean ambiguousPriceOrDiscount = false;

        String source = rawText == null ? "" : rawText;
        Matcher totalMatcher = TOTAL.matcher(source);
        Long printed = totalMatcher.find() ? toCents(totalMatcher.group(1)) : null;
        boolean exactlyOneItem = SINGLE_ITEM_COUNT.matcher(source).find();

        for (String raw : source.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String upper = line.toUpperCase(Locale.ROOT);

            if (upper.matches("^TOTAL\\b.*")) break;
            if (upper.startsWith("SERVED BY:") || upper.matches("^SERVED\\s+BY\\s*:.*")) {
                insideItems = true;
                continue;
            }
            if (upper.matches("^TAX\\s+INV[O0]L?I?CE.*")) {
                insideItems = true;
                continue;
            }
            if (!insideItems || upper.matches("^WHY\\s+PAY\\b.*") || isMetadata(line))
                continue;

            // No guessing from explicit discounts or other unexpected money lines.
            if (upper.matches(".*\\b(?:DISCOUNT|COUPON|VOUCHER|SAVINGS|REFUND)\\b.*")) {
                ambiguousPriceOrDiscount = true;
                continue;
            }

            Matcher inline = INLINE_ITEM.matcher(line);
            if (inline.matches()) {
                int quantity = Integer.parseInt(inline.group(1));
                String name = inline.group(2).trim();
                if (quantity > 0 && containsLetter(name)) {
                    items.add(createItem(quantity, name, toCents(inline.group(3))));
                }
                pendingQuantity = null;
                pendingName = null;
                continue;
            }

            Matcher money = MONEY_ONLY.matcher(line);
            if (money.matches()) {
                if (pendingQuantity != null && pendingName != null) {
                    items.add(createItem(pendingQuantity, pendingName, toCents(money.group(1))));
                    pendingQuantity = null;
                    pendingName = null;
                } else {
                    ambiguousPriceOrDiscount = true;
                }
                continue;
            }

            Matcher nameOnly = ITEM_WITHOUT_PRICE.matcher(line);
            if (nameOnly.matches() && !looksLikeHeaderOrFooter(line)) {
                int quantity = Integer.parseInt(nameOnly.group(1));
                String name = nameOnly.group(2).trim();
                if (quantity > 0 && containsLetter(name)) {
                    pendingQuantity = quantity;
                    pendingName = name;
                    unpricedCandidates++;
                }
                continue;
            }

            // Some OCR results omit BOTH the leading '1' and the printed item
            // price. A single text-only candidate is retained, never auto-priced
            // unless the independently printed item count is exactly one.
            if (!looksLikeHeaderOrFooter(line) && containsLetter(line)
                    && !containsMoney(line) && !line.matches("^\\d+[\\s:./-].*")) {
                pendingQuantity = 1;
                pendingName = line;
                unpricedCandidates++;
            } else if (containsMoney(line)) {
                ambiguousPriceOrDiscount = true;
            }
        }

        if (items.isEmpty() && exactlyOneItem && printed != null && printed > 0
                && unpricedCandidates == 1 && pendingName != null
                && pendingQuantity != null && pendingQuantity == 1
                && !ambiguousPriceOrDiscount) {
            items.add(createItem(1, pendingName, printed));
        }
        return new ParsedReceipt(items, printed);
    }

    private static ReceiptItem createItem(int quantity, String name, long lineTotalCents) {
        long unitPrice = Math.round((double) lineTotalCents / quantity);
        return new ReceiptItem(UUID.randomUUID().toString(), name, name,
                quantity, "ea", unitPrice, Collections.emptyList(), null, false);
    }

    private static boolean containsLetter(String value) {
        return value.matches(".*[A-Za-z].*");
    }

    private static boolean containsMoney(String value) {
        return value.matches(".*\\$\\s*\\d+[.,]\\d{2}.*");
    }

    private static boolean isMetadata(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return upper.contains("CHEMIST WAREHOUSE")
                || upper.contains("VICTORIA STREET")
                || upper.startsWith("GST:") || upper.matches("^TAX\\s+INV.*")
                || upper.startsWith("SERVED BY:")
                || upper.matches("^[A-Z]{3}\\s+\\d{1,2}\\s+\\d{4}.*")
                || upper.startsWith("EFT") || upper.startsWith("CHANGE")
                || upper.contains("INDICATES A TAXABLE ITEM")
                || upper.startsWith("TOTAL INCLUDES GST")
                || upper.matches("^\\d+\\s+ITEM\\(S\\).*$");
    }

    private static boolean looksLikeHeaderOrFooter(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return isMetadata(line) || upper.startsWith("WHY PAY")
                || upper.startsWith("STOP PAYING") || upper.startsWith("PHONE:")
                || upper.startsWith("TOTAL") || upper.startsWith("TAX INVOICE");
    }

    private static long toCents(String amount) {
        return Math.round(Double.parseDouble(amount.replace(',', '.')) * 100);
    }
}
