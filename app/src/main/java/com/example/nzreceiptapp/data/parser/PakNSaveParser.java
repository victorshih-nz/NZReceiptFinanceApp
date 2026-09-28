package com.example.nzreceiptapp.data.parser;

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

/** PAK'nSAVE: product, optional @ quantity/price, optional PF_ item discount. */
public class PakNSaveParser implements IReceiptParser {
    private static final Pattern TOTAL = Pattern.compile("(?im)^\\s*TOTAL(?:\\s+DUE)?\\s+\\$?\\s*(\\d+\\.\\d{2})\\s*$");
    private static final Pattern ITEM = Pattern.compile("^(.+?)\\s+\\$?\\s*(\\d+\\.\\d{2})\\s*(?:[GN*])?$");
    private static final Pattern PF = Pattern.compile("(?i)^PF_\\s*(.*?)\\s+[-−]\\s*\\$?\\s*(\\d+\\.\\d{2})\\s*$");
    private static final Pattern STOP = Pattern.compile("(?i)^(?:\\d+\\s+)?(?:SUB\\s*TOTAL|TOTAL(?:\\s+DUE)?|BALANCE\\s+DUE|EFTPOS|CASH|CHANGE|ROUNDING|GST|VISA|MASTERCARD|TAX INVOICE|THANK YOU|PAYMENT)\\b.*");
    private static final Pattern HEADER = Pattern.compile("(?i)^(?:PAK.?N.?SAVE|STORE\\s+MGR|STORE\\s+\\d+|PH:|PHONE:|WELCOME|\\d{1,2}[./-]\\d{1,2}[./-]\\d{2,4}\\b).*", Pattern.CASE_INSENSITIVE);

    @Override public ParsedReceipt parse(String rawText) {
        List<ReceiptItem> items = new ArrayList<>();
        String pending = null;
        if (rawText != null) for (String raw : rawText.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (STOP.matcher(line).matches()) { pending = null; continue; }
            if (HEADER.matcher(line).matches()) { pending = null; continue; }

            // A discount is never a product. Associate only with the immediately preceding item.
            Matcher pf = PF.matcher(line);
            if (line.regionMatches(true, 0, "PF_", 0, 3)) {
                if (pf.matches() && !items.isEmpty() && pending == null) {
                    ReceiptItem previous = items.get(items.size() - 1);
                    String descriptor = pf.group(1).trim();
                    if (corresponds(descriptor, previous.getCleanedName())) {
                        long discount = Math.round(Double.parseDouble(pf.group(2)) * 100);
                        List<ItemDiscount> discounts = new ArrayList<>(previous.getDiscounts());
                        discounts.add(new ItemDiscount(ItemDiscount.DiscountType.UNKNOWN, line, discount));
                        items.set(items.size() - 1, new ReceiptItem(previous.getId(), previous.getRawName(),
                                previous.getCleanedName(), previous.getQuantity(), previous.getUnit(),
                                previous.getUnitPriceCents(), discounts, previous.getCategory(), previous.getSpecialMk()));
                    }
                }
                // Unmatched PF must not be silently turned into a normal positive-priced item.
                continue;
            }

            // @ has priority over generic amount matching; it may be the second line of an item.
            if (line.contains("@")) {
                QuantityLine quantityLine = QuantityLine.parse(line);
                if (quantityLine != null && pending != null) {
                    items.add(new ReceiptItem(UUID.randomUUID().toString(), pending, pending,
                            quantityLine.quantity, quantityLine.unit, quantityLine.unitPriceCents,
                            Collections.emptyList(), null, false));
                    pending = null;
                } else if (quantityLine != null && !items.isEmpty() && pending == null) {
                    ReceiptItem prev = items.get(items.size() - 1);
                    if (prev.getQuantity() == 1.0 && "ea".equals(prev.getUnit()) &&
                            (quantityLine.printedSubtotalCents == null ||
                                    Math.abs(quantityLine.printedSubtotalCents - prev.getOriginalSubtotalCents()) <= 1)) {
                        items.set(items.size() - 1, new ReceiptItem(prev.getId(), prev.getRawName(),
                                prev.getCleanedName(), quantityLine.quantity, quantityLine.unit,
                                quantityLine.unitPriceCents, prev.getDiscounts(), prev.getCategory(), prev.getSpecialMk()));
                    }
                }
                continue;
            }

            Matcher m = ITEM.matcher(line);
            if (m.matches()) {
                String name = m.group(1).trim();
                if (name.isEmpty()) continue;
                long cents = Math.round(Double.parseDouble(m.group(2)) * 100);
                if (pending != null && (name.matches("(?i).*\\b(?:kg|g|each)\\b.*") || name.length() < 3)) {
                    name = pending + " " + name;
                }
                items.add(new ReceiptItem(UUID.randomUUID().toString(), name, name, 1.0, "ea", cents,
                        Collections.emptyList(), null, false));
                pending = null;
            } else if (!line.matches("(?i).*\\b(?:INVOICE|RECEIPT|TRANSACTION|CARD|TERMINAL|GST|THANK|YOUR)\\b.*")) {
                pending = line;
            }
        }
        Matcher total = TOTAL.matcher(rawText == null ? "" : rawText);
        Long printed = total.find() ? Math.round(Double.parseDouble(total.group(1)) * 100) : null;
        return new ParsedReceipt(items, printed);
    }

    private static boolean corresponds(String pfName, String product) {
        String a = normalise(pfName);
        String b = normalise(product);
        if (a.isEmpty() || b.isEmpty()) return false;
        // PF_ labels are often truncated versions of the preceding product name.
        return b.startsWith(a) || a.startsWith(b) || (a.length() >= 8 && b.contains(a));
    }
    private static String normalise(String text) {
        return text.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
