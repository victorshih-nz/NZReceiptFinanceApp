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

/** Kmart NZ: product name + barcode + price rows, followed by TOTAL. */
public class KmartParser implements IReceiptParser {
    private static final Pattern ITEM = Pattern.compile(
            "^\\s*[%?]?\\s*(.+?)\\s+(\\d{10,14})\\s+\\$?(\\d+\\.\\d{2})\\s*$");
    private static final Pattern TOTAL = Pattern.compile(
            "(?im)^\\s*TOTAL\\s+\\$?\\s*(\\d+\\.\\d{2})\\s*$");

    @Override public ParsedReceipt parse(String rawText) {
        List<ReceiptItem> items = new ArrayList<>();
        if (rawText != null) {
            for (String raw : rawText.split("\\r?\\n")) {
                Matcher m = ITEM.matcher(raw.trim());
                if (!m.matches()) continue;
                String name = m.group(1).trim();
                long cents = toCents(m.group(3));
                items.add(new ReceiptItem(UUID.randomUUID().toString(), name, name,
                        1.0, "ea", cents, Collections.emptyList(), null, false));
            }
        }
        Matcher total = TOTAL.matcher(rawText == null ? "" : rawText);
        Long printed = total.find() ? toCents(total.group(1)) : null;
        return new ParsedReceipt(items, printed);
    }

    private static long toCents(String amount) {
        return Math.round(Double.parseDouble(amount) * 100);
    }
}
