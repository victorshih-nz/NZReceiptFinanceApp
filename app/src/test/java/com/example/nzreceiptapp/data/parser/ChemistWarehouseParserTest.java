package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.assertEquals;

import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import org.junit.Test;

public class ChemistWarehouseParserTest {

    @Test
    public void parsesInlineItemAndIgnoresWhyPay() {
        String text =
                "CHEMIST WAREHOUSE\n" +
                "Tax Invoice\n" +
                "1 ECOSTORE U/SEN SHAMPOO NEW* $8.99\n" +
                "Why Pay $9.49?\n" +
                "TOTAL $8.99\n";

        ParsedReceipt result = new ChemistWarehouseParser().parse(text);

        assertEquals(1, result.getItems().size());
        assertEquals("ECOSTORE U/SEN SHAMPOO NEW*",
                result.getItems().get(0).getName());
        assertEquals(899, result.getItems().get(0).getUnitPriceCents());
        assertEquals(Long.valueOf(899), result.getPrintedTotalCents());
    }

    @Test
    public void parsesSplitNameAndPriceRowsFromLayoutReconstruction() {
        String text =
                "CHEMIST WAREHOUSE Auckland - Victoria Street\n" +
                "Tax Invoice\n" +
                "1 ECOSTORE U/SEN SHAMPOO NEW*\n" +
                "$8.99\n" +
                "Why Pay $9.49?\n" +
                "TOTAL\n" +
                "$8.99\n";

        ParsedReceipt result = new ChemistWarehouseParser().parse(text);

        assertEquals(1, result.getItems().size());
        assertEquals("ECOSTORE U/SEN SHAMPOO NEW*",
                result.getItems().get(0).getName());
        assertEquals(899, result.getItems().get(0).getFinalSubtotalCents());
    }
}
