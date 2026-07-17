package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;

import org.junit.Test;

import java.util.List;

public class PakNSaveParserTest {

    @Test
    public void testParsePakNSaveReceipt() {
        String mockOcrText = "PAK'nSAVE ROYAL OAK\n" +
                "STORE MGR: ANTONY DENTON\n" +
                "PAMS MILK 2L                  3.89\n" +
                "CHOCOLATE BLOCK               5.50\n" +
                "BANANAS LOOSE\n" +
                "0.645 kg @ $3.49 /kg          2.25\n" +
                "PAMS FLOUR 1KG                2.60\n" +
                "SUBTOTAL                     14.24\n" +
                "TOTAL DUE                   $14.24\n" +
                "EFTPOS                       14.24\n" +
                "TAX INVOICE  GST INCL        1.86\n" +
                "16.07.26 14:32  0145 02 4452\n" +
                "THANK YOU FOR SHOPPING WITH US";

        PakNSaveParser parser = new PakNSaveParser();
        List<ReceiptItem> items = parser.parseRawText(mockOcrText);

        // 1. 驗證商品清單 List 物件是否存在，且品項總數是否正確
        assertNotNull(items);
        assertEquals(4, items.size());

        // 2. 驗證第一個正常品項 (PAMS MILK 2L)
        ReceiptItem item1 = items.get(0);
        assertEquals("PAMS MILK 2L", item1.getRawName());
        assertEquals(1.0, item1.getQuantity(), 0.001);
        assertEquals("ea", item1.getUnit());
        assertEquals(389L, item1.getUnitPriceCents());

        // 3. 驗證第二個正常品項 (CHOCOLATE BLOCK - 可自由選擇是否加強驗證)
        ReceiptItem item2 = items.get(1);
        assertEquals("CHOCOLATE BLOCK", item2.getRawName());
        assertEquals(550L, item2.getUnitPriceCents());

        // 4. 驗證第三個秤重品項 (BANANAS LOOSE)
        ReceiptItem item3 = items.get(2);
        assertEquals("BANANAS LOOSE", item3.getRawName());
        assertEquals(0.645, item3.getQuantity(), 0.001);
        assertEquals("kg", item3.getUnit());
        assertEquals(349L, item3.getUnitPriceCents());

        // 5. 驗證第四個正常品項 (PAMS FLOUR 1KG)
        ReceiptItem item4 = items.get(3);
        assertEquals("PAMS FLOUR 1KG", item4.getRawName());
        assertEquals(260L, item4.getUnitPriceCents());
    }
}