package com.example.nzreceiptapp.domain.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Collections;

public class ReceiptTest {

    @Test
    public void constructor_normalizesPurchaseTimestampToSecondPrecision() {
        LocalDateTime timestamp = LocalDateTime.of(
                2026, 8, 18, 10, 5, 12, 987_654_321);

        Receipt receipt = new Receipt(
                "receipt",
                new Store("store", "Woolworths", "Greenlane"),
                Collections.emptyList(),
                timestamp,
                0,
                false);

        assertEquals(timestamp.withNano(0), receipt.getPurchaseDate());
    }

    @Test
    public void constructor_keepsMissingPurchaseTimestampNull() {
        Receipt receipt = new Receipt(
                "receipt",
                new Store("store", "Woolworths", ""),
                Collections.emptyList(),
                null,
                0,
                false);

        assertNull(receipt.getPurchaseDate());
    }
}
