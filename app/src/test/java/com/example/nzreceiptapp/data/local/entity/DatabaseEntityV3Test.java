package com.example.nzreceiptapp.data.local.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDateTime;

public class DatabaseEntityV3Test {

    @Test
    public void storeEntity_constructionStoresNormalizedIdentity() {
        StoreEntity entity = new StoreEntity(
                "store-1", " Wool-worths! ", " Green Lane! ");

        assertEquals(" Wool-worths! ", entity.chainName);
        assertEquals(" Green Lane! ", entity.branchName);
        assertEquals("woolworths", entity.normalizedChain);
        assertEquals("greenlane", entity.normalizedBranch);
    }

    @Test
    public void receiptEntity_roomConstructionStoresSavedSequence() {
        ReceiptEntity entity = new ReceiptEntity(
                "receipt-1",
                "store-1",
                LocalDateTime.of(2026, 8, 17, 10, 5, 12),
                0,
                false,
                "OCR",
                "image.jpg",
                100L,
                7);

        assertEquals(7, entity.savedSequence);
    }
}
