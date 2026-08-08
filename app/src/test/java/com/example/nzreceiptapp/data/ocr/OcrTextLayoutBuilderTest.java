package com.example.nzreceiptapp.data.ocr;

import static org.junit.Assert.assertEquals;

import com.example.nzreceiptapp.data.parser.WoolworthsParser;
import com.example.nzreceiptapp.domain.model.ReceiptItem;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class OcrTextLayoutBuilderTest {

    private final OcrTextLayoutBuilder builder = new OcrTextLayoutBuilder();

    @Test
    public void build_joinsSplitColumnsAndOrdersRows() {
        List<OcrTextLayoutBuilder.Fragment> fragments = Arrays.asList(
                fragment("7.99", 420, 143, 470, 163),
                fragment("Carrot", 20, 100, 100, 120),
                fragment("1.01", 420, 123, 470, 143),
                fragment("WW Canola Oil 2L", 20, 140, 230, 160),
                fragment("0.520 kg NET @ $1.95/kg", 35, 120, 280, 140)
        );

        assertEquals(
                "Carrot\n"
                        + "0.520 kg NET @ $1.95/kg 1.01\n"
                        + "WW Canola Oil 2L 7.99",
                builder.build(fragments));
    }

    @Test
    public void build_toleratesSmallVerticalDifferenceAcrossWideReceipt() {
        List<OcrTextLayoutBuilder.Fragment> fragments = Arrays.asList(
                fragment("WW Milk 2L", 20, 200, 180, 224),
                fragment("3.89", 430, 207, 480, 231)
        );

        assertEquals("WW Milk 2L 3.89", builder.build(fragments));
    }

    @Test
    public void rebuiltRows_canBeParsedByExistingWoolworthsParser() {
        List<OcrTextLayoutBuilder.Fragment> fragments = Arrays.asList(
                fragment("Carrot", 20, 100, 100, 118),
                fragment("0.520 kg NET @ $1.95/kg", 35, 120, 280, 138),
                fragment("1.01", 420, 122, 470, 140),
                fragment("WW Canola Oil 2L", 20, 142, 230, 160),
                fragment("7.99", 420, 144, 470, 162)
        );

        String rebuiltText = builder.build(fragments);
        List<ReceiptItem> items = new WoolworthsParser().parseRawText(rebuiltText);

        assertEquals(2, items.size());
        assertEquals("Carrot", items.get(0).getName());
        assertEquals(101, items.get(0).getOriginalSubtotalCents());
        assertEquals("WW Canola Oil 2L", items.get(1).getName());
        assertEquals(799, items.get(1).getOriginalSubtotalCents());
    }

    @Test
    public void build_ignoresBlankAndInvalidFragments() {
        List<OcrTextLayoutBuilder.Fragment> fragments = Arrays.asList(
                fragment("TOTAL", 20, 100, 100, 120),
                fragment("   ", 120, 100, 180, 120),
                fragment("95.00", 420, 100, 470, 120),
                fragment("ignored", 10, 50, 10, 80),
                null
        );

        assertEquals("TOTAL 95.00", builder.build(fragments));
    }

    private OcrTextLayoutBuilder.Fragment fragment(
            String text, int left, int top, int right, int bottom) {
        return new OcrTextLayoutBuilder.Fragment(text, left, top, right, bottom);
    }
}
