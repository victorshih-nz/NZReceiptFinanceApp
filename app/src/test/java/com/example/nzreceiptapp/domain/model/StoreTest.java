package com.example.nzreceiptapp.domain.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

public class StoreTest {

    @Test
    public void normalizedChain_trimsAndLowercasesWithoutChangingDisplayValue() {
        Store store = new Store("store-id", " Woolworths ", "Greenlane");

        assertEquals("woolworths", store.getNormalizedChainName());
        assertEquals(" Woolworths ", store.getChainName());
    }

    @Test
    public void normalizedChain_nullReturnsEmpty() {
        Store store = new Store("store-id", null, "Greenlane");

        assertEquals("", store.getNormalizedChainName());
    }

    @Test
    public void normalizedChain_null(){
        Store store = new Store("store-id", null, null);

        assertEquals("",store.getNormalizedBranchName());
        assertEquals("", store.getNormalizedChainName());
    }

    @Test
    public void normalizedChain_whitespaceReturnsEmpty() {
        Store store = new Store("store-id", "   ", "Greenlane");

        assertEquals("", store.getNormalizedChainName());
    }

    @Test
    public void normalizedBranch_trimsAndLowercasesWithoutChangingDisplayValue() {
        Store store = new Store("store-id", "Woolworths", " GREENLANE ");

        assertEquals("greenlane", store.getNormalizedBranchName());
        assertEquals(" GREENLANE ", store.getBranchName());
    }

    @Test
    public void normalizedBranch_emptyFormsReturnSameIdentity() {
        assertEquals("", new Store("null", "Woolworths", null)
                .getNormalizedBranchName());
        assertEquals("", new Store("empty", "Woolworths", "")
                .getNormalizedBranchName());
        assertEquals("", new Store("whitespace", "Woolworths", "   ")
                .getNormalizedBranchName());
    }

    @Test
    public void normalization_removesInternalSpaces() {
        Store store = new Store("store-id", " New   World ", "Greenlane");

        assertEquals("newworld", store.getNormalizedChainName());
    }

    @Test
    public void normalization_removesPunctuation() {
        Store store = new Store("store-id", " PAK'nSAVE ", "Greenlane");

        assertEquals("paknsave", store.getNormalizedChainName());
    }

    @Test
    public void normalization_keepsAsciiLettersAndDigitsOnly() {
        Store store = new Store("store-id", "Store #42!", "Mt. Eden 2");

        assertEquals("store42", store.getNormalizedChainName());
        assertEquals("mteden2", store.getNormalizedBranchName());
    }

    @Test
    public void normalization_isIdempotent() {
        Store firstStore = new Store("first", "Woolworths", " Greenlane ");
        Store secondStore = new Store(
                "second", "Woolworths", firstStore.getNormalizedBranchName());

        assertEquals(firstStore.getNormalizedBranchName(),
                secondStore.getNormalizedBranchName());
    }

    @Test
    public void normalization_isIndependentOfDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            Store store = new Store("store-id", "I", "I");

            assertEquals("i", store.getNormalizedChainName());
            assertEquals("i", store.getNormalizedBranchName());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
