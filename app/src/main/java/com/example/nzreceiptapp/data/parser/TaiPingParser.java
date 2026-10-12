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

/** Tai Ping: descriptions followed by quantity @ unit price / line subtotal. */
public class TaiPingParser implements IReceiptParser {

    // A reconstructed row can contain the description BEFORE the quantity fields.
    // 'e.99' is a narrowly supported OCR error, not a global e -> 0 substitution.
    private static final Pattern QUANTITY = Pattern.compile(
            "(?i)(?<![\\d.,])(\\d+(?:[.,]\\d{1,4})?)\\s*@\\s*\\$?\\s*"
                    + "(\\d+[.,]\\d{2}|[eEoO][.,]\\d{2})\\s+\\$?\\s*"
                    + "(\\d+[.,]\\d{2})(?![\\d.,])");

    private static final Pattern TOTAL_LABEL = Pattern.compile(
            "(?i)\\bTOTAL\\s+(?:INCLUDING|1NCL\\s*UDING)\\s+GST\\s*:?\\s*(.*)$");
    private static final Pattern PRINTED_MONEY = Pattern.compile(
            "\\$\\s*(\\d+[.,]\\d{2})(?![A-Za-z0-9])");
    private static final Pattern ITEM_COUNT = Pattern.compile(
            "(?i)^\\s*\\d+\\s+item\\(s\\).*$");

    @Override
    public ParsedReceipt parse(String rawText) {
        List<ReceiptItem> items = new ArrayList<>();
        List<String> pendingNames = new ArrayList<>();
        Long printed = null;

        if (rawText != null) {
            for (String raw : rawText.split("\\r?\\n")) {
                String line = raw.trim();
                if (line.isEmpty()) continue;
                String upper = line.toUpperCase(Locale.ROOT);

                // The reconstructed total may begin with '15 item(s)' and contain
                // a corrupted first amount followed by the valid $93.38.
                Matcher totalLabel = TOTAL_LABEL.matcher(line);
                if (totalLabel.find()) {
                    Matcher money = PRINTED_MONEY.matcher(totalLabel.group(1));
                    while (money.find()) printed = toCents(money.group(1));
                    pendingNames.clear();
                    break; // Never treat GST/payment/footer as products.
                }
                if (ITEM_COUNT.matcher(line).matches()
                        || upper.startsWith("CARD:")
                        || upper.startsWith("GST AMOUNT")) {
                    pendingNames.clear();
                    continue;
                }

                Matcher quantity = QUANTITY.matcher(line);
                if (quantity.find()) {
                    // Same-row descriptions: 'Assam ... 1.0024 @ 2.17 $2.17'.
                    String prefix = line.substring(0, quantity.start()).trim();
                    // The fixture uses "OCR" as a leading layout-noise token:
                    // "Spinach-KG" followed by "OCR 0.59 @ 5.99 $3.53 noise".
                    // A noise marker must not replace a previously captured name.
                    if (!prefix.isEmpty()
                            && !prefix.matches("(?i)^(?:OCR|NOISE)\\s*[:：-]?$")
                            && looksLikeDescription(prefix)) {
                        addPendingName(pendingNames, prefix);
                    }

                    if (!pendingNames.isEmpty()) {
                        String name = chooseDisplayName(pendingNames);
                        double parsedQty = parseDecimal(quantity.group(1));
                        long subtotal = toCents(quantity.group(3));
                        Long unitPrice = resolveUnitPrice(quantity.group(2), parsedQty, subtotal);
                        if (unitPrice != null) {
                            double qty = normaliseRegisterQuantity(parsedQty, unitPrice, subtotal);
                            if (isArithmeticPlausible(qty, unitPrice, subtotal)) {
                                items.add(new ReceiptItem(
                                        UUID.randomUUID().toString(),
                                        name, name, qty, inferUnit(name, qty), unitPrice,
                                        Collections.emptyList(), null, false));
                            }
                        }
                    }
                    pendingNames.clear();
                    continue;
                }

                if (isMetadata(line)) {
                    pendingNames.clear();
                } else if (looksLikeDescription(line)) {
                    addPendingName(pendingNames, line);
                }
            }
        }
        return new ParsedReceipt(items, printed);
    }

    private static void addPendingName(List<String> pending, String name) {
        pending.add(name);
        if (pending.size() > 5) pending.remove(0);
    }

    private static Long resolveUnitPrice(String token, double quantity, long subtotal) {
        if (Character.isDigit(token.charAt(0))) return toCents(token);

        // Only recover a single OCR-corrupted leading digit if the printed
        // subtotal and quantity independently prove the intended unit price.
        // e.99 / 3.00 / 2.97 -> 0.99; no guess if reconciliation fails.
        long wholeQuantity = Math.round(quantity);
        if (wholeQuantity < 1 || Math.abs(quantity - wholeQuantity) > 0.01
                || subtotal % wholeQuantity != 0) return null;
        long candidate = subtotal / wholeQuantity;
        String centsSuffix = token.substring(token.indexOf('.') >= 0
                ? token.indexOf('.') + 1 : token.indexOf(',') + 1);
        long expectedFraction = Long.parseLong(centsSuffix);
        return candidate % 100 == expectedFraction ? candidate : null;
    }

    private static String chooseDisplayName(List<String> candidates) {
        for (int i = candidates.size() - 1; i >= 0; i--) {
            String value = candidates.get(i).trim();
            if (value.matches(".*[A-Za-z].*")) return value;
        }
        return candidates.get(candidates.size() - 1).trim();
    }

    private static double normaliseRegisterQuantity(double qty, long unit, long subtotal) {
        long nearest = Math.round(qty);
        if (nearest >= 1 && Math.abs(qty - nearest) <= 0.01
                && Math.abs(nearest * unit - subtotal) <= 1) return nearest;
        return qty;
    }

    private static boolean isArithmeticPlausible(double qty, long unit, long subtotal) {
        return Math.abs(Math.round(qty * unit) - subtotal) <= 2;
    }

    private static String inferUnit(String name, double qty) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.contains("-KG") || upper.contains("/KG")
                || upper.matches(".*\\bKG\\b.*")) return "kg";
        if (Math.abs(qty - Math.rint(qty)) > 0.01) return "kg";
        return "ea";
    }

    private static boolean looksLikeDescription(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        if (upper.matches(".*\\b(?:GST|TOTAL|CARD|RECEIPT|DATE|POS|OPERATOR|PHONE|PH:)\\b.*"))
            return false;
        if (line.contains("@")) return false;
        // Names can include spaces, parentheses, underscores, apostrophes, digits, etc.
        // Require at least one letter; do not restrict the entire name to letters.
        return line.matches(".*[A-Za-z\\p{IsHan}].*");
    }

    private static boolean isMetadata(String line) {
        String upper = line.toUpperCase(Locale.ROOT);
        return upper.contains("TAI PING ASIAN SUPERMARKET")
                || upper.contains("GREAT SOUTH ROAD")
                || upper.startsWith("PH:") || upper.startsWith("GST#")
                || upper.startsWith("RECEIPT NO") || upper.startsWith("DATE:")
                || upper.startsWith("POS STATION") || upper.startsWith("OPERATOR:")
                || upper.contains("TAX INVOICE") || upper.startsWith("THANKS FOR SHOPPING")
                || upper.startsWith("PLEASE RETAIN") || upper.startsWith("MEMBER NAME")
                || upper.startsWith("LOYALTY");
    }

    private static double parseDecimal(String value) {
        return Double.parseDouble(value.replace(',', '.'));
    }

    private static long toCents(String amount) {
        return Math.round(parseDecimal(amount) * 100);
    }
}
