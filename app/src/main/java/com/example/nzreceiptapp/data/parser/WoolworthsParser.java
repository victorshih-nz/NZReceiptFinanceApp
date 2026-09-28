package com.example.nzreceiptapp.data.parser;

import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.parser.IReceiptParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Woolworths: first product line may contain total; @ continuation supplies unit/quantity. */
public class WoolworthsParser implements IReceiptParser {
    private static final Pattern ITEM = Pattern.compile("^([\\^*#]?)\\s*(.+?)\\s+\\$?(\\d+\\.\\d{2})\\s*(?:[GN*])?\\s*$");
    private static final Pattern TOTAL = Pattern.compile("(?im)^\\s*TOTAL(?:\\s+DUE)?\\s+\\$?\\s*(\\d+\\.\\d{2})\\s*$");
    private static final Pattern STOP = Pattern.compile("(?i)^(?:\\d+\\s+)?(?:SUB\\s*TOTAL|TOTAL(?:\\s+DUE)?|EFTPOS|CASH|CHANGE|ROUNDING|GST|VISA|MASTERCARD|PAYMENT)\\b.*");

    @Override public ParsedReceipt parse(String rawText) {
        List<ReceiptItem> items = new ArrayList<>();
        String pending = null;
        boolean pendingSpecial = false;
        if (rawText != null) for (String raw : rawText.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (STOP.matcher(line).matches() || isMetadata(line)) {
                pending = null; pendingSpecial = false; continue;
            }
            // Recognise '@' before considering line-ending monetary amounts.
            if (line.contains("@")) {
                // Some old OCR fixtures embed a name before the @ continuation and a trailing total.
                int qtyStart = findQuantityStart(line);
                String quantityPart = qtyStart >= 0 ? line.substring(qtyStart).trim() : line;
                QuantityLine q = QuantityLine.parse(quantityPart);
                if (q != null) {
                    String inlineName = qtyStart > 0 ? line.substring(0, qtyStart).trim() : "";
                    if (pending != null || !inlineName.isEmpty()) {
                        String name = pending != null ? pending : inlineName.replaceFirst("^[\\^*#]\\s*", "");
                        boolean isSpecial = pendingSpecial || inlineName.startsWith("^");
                        items.add(new ReceiptItem(UUID.randomUUID().toString(), name, name,
                                q.quantity, q.unit, q.unitPriceCents, Collections.emptyList(), null, isSpecial));
                        pending = null; pendingSpecial = false;
                    } else if (!items.isEmpty()) {
                        ReceiptItem prev = items.get(items.size() - 1);
                        if (prev.getQuantity() == 1.0 && "ea".equals(prev.getUnit()) &&
                                (q.printedSubtotalCents == null ||
                                        Math.abs(prev.getOriginalSubtotalCents() - q.printedSubtotalCents) <= 1) &&
                                Math.abs(prev.getOriginalSubtotalCents() - Math.round(q.quantity * q.unitPriceCents)) <= 1) {
                            items.set(items.size() - 1, new ReceiptItem(prev.getId(), prev.getRawName(),
                                    prev.getCleanedName(), q.quantity, q.unit, q.unitPriceCents,
                                    prev.getDiscounts(), prev.getCategory(), prev.getSpecialMk()));
                        }
                    }
                }
                // Never reinterpret a failed @ line as a new product.
                continue;
            }
            Matcher item = ITEM.matcher(line);
            if (item.matches()) {
                String prefix = item.group(1);
                String name = item.group(2).trim();
                if (pending != null && name.length() < 3) name = pending + " " + name;
                long cents = Math.round(Double.parseDouble(item.group(3)) * 100);
                items.add(new ReceiptItem(UUID.randomUUID().toString(), name, name, 1.0, "ea", cents,
                        Collections.emptyList(), null, "^".equals(prefix) || pendingSpecial));
                pending = null; pendingSpecial = false;
            } else if (line.length() > 2) {
                pendingSpecial = line.startsWith("^");
                pending = line.replaceFirst("^[\\^*#]\\s*", "").trim();
            }
        }
        Matcher total = TOTAL.matcher(rawText == null ? "" : rawText);
        Long printed = total.find() ? Math.round(Double.parseDouble(total.group(1)) * 100) : null;
        return new ParsedReceipt(items, printed);
    }

    private static int findQuantityStart(String line) {
        Matcher matcher = Pattern.compile("(?i)(?:Qty\\s*)?\\d+(?:[ .]\\d{1,3})?\\s*(?:kg|g)?\\s*(?:NET\\s*)?@").matcher(line);
        int start = -1;
        while (matcher.find()) start = matcher.start();
        return start;
    }
    private static boolean isMetadata(String line) {
        String u = line.toUpperCase(java.util.Locale.ROOT);
        return u.startsWith("STORE ") || u.contains("TAX INVOICE") || u.contains("WELCOME") ||
                u.contains("THANK YOU") || u.contains("DUPLICATE") || u.contains("GREENLANE") ||
                u.contains("GREAT SOUTH ROAD") || u.contains("VISA DEBIT") || u.contains("NZ$") ||
                u.contains("TERM ID") || u.contains("PH:") || u.contains("WOOLWORTHS") ||
                u.contains("PAK'NSAVE") || u.startsWith("CARD:") || u.startsWith("TVR ");
    }
}
