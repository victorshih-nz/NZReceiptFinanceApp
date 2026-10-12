package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.*;
import org.junit.Test;

public class ParserProviderTest {
    private final ParserProvider provider = new ParserProvider();

    @Test public void getParser_returnsParserForSupportedChains() {
        assertTrue(provider.getParser("Woolworths") instanceof WoolworthsParser);
        assertTrue(provider.getParser("Countdown") instanceof WoolworthsParser);
        assertTrue(provider.getParser("PAK'nSAVE") instanceof PakNSaveParser);
        assertTrue(provider.getParser("Kmart") instanceof KmartParser);
        assertTrue(provider.getParser("Tai Ping") instanceof TaiPingParser);
        assertTrue(provider.getParser("IKEA") instanceof IkeaParser);
        assertTrue(provider.getParser("Chemist Warehouse") instanceof ChemistWarehouseParser);
        assertNull(provider.getParser("Unknown Store"));
    }

    @Test public void detectChain_returnsCanonicalName() {
        assertEquals("Woolworths", provider.detectChain("WOOLWORTHS NZ"));
        assertEquals("Woolworths", provider.detectChain("Countdown receipt"));
        assertEquals("PAK'nSAVE", provider.detectChain("PAK'nSAVE ROYAL OAK"));
        assertEquals("Kmart", provider.detectChain("KMART 8229 SYLVIA PARK"));
        assertEquals("Tai Ping", provider.detectChain("TAI PING ASIAN SUPERMARKET"));
        assertEquals("IKEA", provider.detectChain("IKEA New Zealand Ltd"));
        assertEquals("Chemist Warehouse", provider.detectChain("CHEMIST WAREHOUSE"));
        assertNull(provider.detectChain("Unknown receipt"));
    }
}
