package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.assertEquals;

import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import org.junit.Test;

public class TaiPingParserTest {

    @Test
    public void parsesMultipleBilingualItemsIncludingBroccoliAndLaterRows() {
        String text =
                "TAI PING ASIAN SUPERMARKET\n" +
                "TAX INVOICE\n" +
                "黄芽白\n" +
                "Chinese cabbage\n" +
                "1.00 @ 1.99 $1.99\n" +
                "西兰花(个)\n" +
                "Broccoli(Each)\n" +
                "3.00 @ 0.99 $2.97\n" +
                "螺霸王螺蛳粉_280g\n" +
                "Luobawang Liuzhou River Snails Flavour R...\n" +
                "1.0024 @ 3.99 $3.99\n" +
                "韩国乌冬面 200g\n" +
                "Korean udong noodle 200g\n" +
                "1.00 @ 0.99 $0.99\n" +
                "4 item(s)\n" +
                "Total including GST: $9.94";

        ParsedReceipt result = new TaiPingParser().parse(text);

        assertEquals(4, result.getItems().size());

        assertEquals("Chinese cabbage", result.getItems().get(0).getName());

        assertEquals("Broccoli(Each)", result.getItems().get(1).getName());
        assertEquals(3.0, result.getItems().get(1).getQuantity(), 0.0001);
        assertEquals(99, result.getItems().get(1).getUnitPriceCents());

        assertEquals("Luobawang Liuzhou River Snails Flavour R...",
                result.getItems().get(2).getName());
        assertEquals(1.0, result.getItems().get(2).getQuantity(), 0.0001);

        assertEquals("Korean udong noodle 200g",
                result.getItems().get(3).getName());

        assertEquals(Long.valueOf(994), result.getPrintedTotalCents());
    }

    @Test
    public void findsQuantityInsideNoisyReconstructedRow() {
        String text =
                "TAI PING ASIAN SUPERMARKET\n" +
                "TAX INVOICE\n" +
                "Spinach-KG\n" +
                "OCR 0.59 @ 5.99 $3.53 noise\n" +
                "1 item(s)\n" +
                "Total including GST: $3.53";

        ParsedReceipt result = new TaiPingParser().parse(text);

        assertEquals(1, result.getItems().size());
        assertEquals("Spinach-KG", result.getItems().get(0).getName());
        assertEquals(0.59, result.getItems().get(0).getQuantity(), 0.0001);
        assertEquals("kg", result.getItems().get(0).getUnit());
    }
}
