package com.example.nzreceiptapp.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser output before store/date information is added to the final Receipt.
 */
public final class ParsedReceipt {
    private final List<ReceiptItem> items;
    private final Long printedTotalCents;

    public ParsedReceipt(List<ReceiptItem> items, Long printedTotalCents) {
        this.items = items == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(items));
        this.printedTotalCents = printedTotalCents;
    }

    public List<ReceiptItem> getItems() {
        return items;
    }

    public Long getPrintedTotalCents() {
        return printedTotalCents;
    }

}
