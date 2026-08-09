package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ParserProviderTest {

    private final ParserProvider provider = new ParserProvider();

    @Test
    public void getParser_returnsParserForSupportedChains() {
        assertTrue(provider.getParser("Woolworths") instanceof WoolworthsParser);
        assertTrue(provider.getParser("Countdown") instanceof WoolworthsParser);
        assertTrue(provider.getParser("PAK'nSAVE") instanceof PakNSaveParser);
        assertNull(provider.getParser("Unknown Store"));
    }

    @Test
    public void detectChain_returnsCanonicalName() {
        assertEquals("Woolworths", provider.detectChain("WOOLWORTHS NZ"));
        assertEquals("Woolworths", provider.detectChain("Countdown receipt"));
        assertEquals("PAK'nSAVE", provider.detectChain("PAK'nSAVE ROYAL OAK"));
        assertNull(provider.detectChain("Unknown receipt"));
    }
}
