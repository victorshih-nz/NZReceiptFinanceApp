package com.example.nzreceiptapp.data.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Recognises a quantity/price continuation. Match @ first, THEN distinguish weight units. */
final class QuantityLine {
    private static final Pattern INFO = Pattern.compile(
            "(?i)^(?:Qty\\s*)?(\\d+(?:[ .]\\d{1,3})?)\\s*(kg|g)?\\s*(?:NET\\s*)?@\\s*\\$?(\\d+\\.\\d{2})(?:\\s*/\\s*(kg|g)|\\s+each)?(?:\\s+\\$?(\\d+\\.\\d{2}))?\\s*(?:[GN*])?\\s*$");
    final double quantity;
    final String unit;
    final long unitPriceCents;
    final Long printedSubtotalCents;
    private QuantityLine(double quantity, String unit, long unitPriceCents, Long subtotal) {
        this.quantity = quantity;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.printedSubtotalCents = subtotal;
    }
    static QuantityLine parse(String line) {
        if (line == null || !line.contains("@")) return null;
        Matcher m = INFO.matcher(line.trim());
        if (!m.matches()) return null;
        String quantityText = m.group(1);
        // Woolworths OCR can render 0.520 as '0 520'. Only normalise the unambiguous 0 xxx form.
        if (quantityText.matches("0 \\d{1,3}")) quantityText = quantityText.replace(' ', '.');
        else if (quantityText.contains(" ")) return null;
        double quantity = Double.parseDouble(quantityText);
        if (quantity <= 0) return null;
        String weight = m.group(2) != null ? m.group(2) : m.group(4);
        String unit = weight == null ? "ea" : weight.toLowerCase(java.util.Locale.ROOT);
        long unitPrice = Math.round(Double.parseDouble(m.group(3)) * 100);
        Long subtotal = m.group(5) == null ? null : Math.round(Double.parseDouble(m.group(5)) * 100);
        if (subtotal != null && Math.abs(Math.round(quantity * unitPrice) - subtotal) > 1) return null;
        return new QuantityLine(quantity, unit, unitPrice, subtotal);
    }
}
