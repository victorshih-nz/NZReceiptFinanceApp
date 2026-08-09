package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class WoolworthsParserTest {

    private WoolworthsParser parser;

    @Before
    public void setUp() {
        parser = new WoolworthsParser();
    }

    @Test
    public void testParseReceipt1_StandardAndWeightedItems() {
        // 模擬第一張收據：包含標準單件品項、行尾帶有GST代碼(G/N)、以及單行複雜重量計價
        String mockOcrText = "WOOLWORTHS AUCKLAND\n" +
                "STORE 4321 - TAX INVOICE\n" +
                "WW MILK HOMOGENISED 2L       3.89 G\n" + // 標準行 + 空格 + 金額 + G
                "HAMPTONS BRIE 125G           4.50 N\n" + // 標準行 + 免稅代碼 N
                "BANANAS LOOSE 0.645 kg @ $3.49 /kg   2.25 G\n" + // 單行內嵌重量與單價明細
                "SUBTOTAL                     10.64\n" +
                "TOTAL                        10.64\n" +
                "EFTPOS                       10.64\n";

        ParsedReceipt parsed = parser.parse(mockOcrText);
        List<ReceiptItem> items = parsed.getItems();

        assertNotNull(items);
        assertEquals(3, items.size());
        assertEquals(Long.valueOf(1064), parsed.getPrintedTotalCents());

        // 1. 驗證牛奶 (標準單件)
        ReceiptItem milk = items.get(0);
        assertEquals("WW MILK HOMOGENISED 2L", milk.getName());
        assertEquals(1.0, milk.getQuantity(), 0.001);
        assertEquals("ea", milk.getUnit());
        assertEquals(389, milk.getUnitPriceCents());
        assertEquals(389, milk.getOriginalSubtotalCents());
        assertNull("Format parsers must not assign categories", milk.getCategory());

        // 2. 驗證起司 (免稅標記 N)
        ReceiptItem brie = items.get(1);
        assertEquals("HAMPTONS BRIE 125G", brie.getName());
        assertEquals(450, brie.getOriginalSubtotalCents());

        // 3. 驗證香蕉 (單行內嵌重量計價)
        ReceiptItem bananas = items.get(2);
        assertTrue(bananas.getName().contains("BANANAS LOOSE"));
        assertEquals(0.645, bananas.getQuantity(), 0.001);
        assertEquals("kg", bananas.getUnit());
        assertEquals(349, bananas.getUnitPriceCents());
        assertEquals(225, bananas.getOriginalSubtotalCents());
    }

    @Test
    public void testParseReceipt2_MultilineMultiBuy() {
        String mockOcrText = "WOOLWORTHS METRO\n" +
                "CORN CHIPS 150G              3.00 *\n" +
                "  2 @ 1.50\n" +
                "WW WATER 24PK                9.00 G\n" +
                "TOTAL DUE                   12.00\n" +
                "CASH                         20.00\n";

        ParsedReceipt parsed = parser.parse(mockOcrText);
        List<ReceiptItem> items = parsed.getItems();

        assertNotNull(items);
        assertEquals(2, items.size());
        assertEquals(Long.valueOf(1200), parsed.getPrintedTotalCents());

        // 1. 驗證多件折落的洋芋片
        ReceiptItem chips = items.get(0);
        assertEquals("CORN CHIPS 150G", chips.getName());
        assertEquals(2.0, chips.getQuantity(), 0.001);
        assertEquals("ea", chips.getUnit());
        assertEquals(150, chips.getUnitPriceCents());
        assertEquals(300, chips.getOriginalSubtotalCents());

        // 2. 驗證水
        ReceiptItem water = items.get(1);
        assertEquals("WW WATER 24PK", water.getName());
        assertEquals(900, water.getOriginalSubtotalCents());
    }

    @Test
    public void testParseRealOCRText(){
        String mockOcrText = "Woolworths\n" +
                "3128 Greenlane PH: 09 522 6970\n" +
                "326 Great South Road\n" +
                "Tax Invoice/Credit Note - GST No. 44-833-938\n" +
                "\n" +
                "Carrot\n" +
                "0 520 kg NET @ $1.95/kg                  1.01\n" +
                "Ww Canola Oil 2L                           7.99\n" +
                "^ Keri Apple Orange Mango Drink 1L         1.99\n" +
                "^ Keri Pulpy Orange Drink 1L\n" +
                "Qty   2 @ $1.99 each                       3.98\n" +
                "Ww Soda Water 1.5L                         1.79\n" +
                "Essentials French Fries 1kg                3.10\n" +
                "^ Auntie Dais Dumpling Chick n Cori 600g   9.90\n" +
                "Ww Chicken DrumsticksLarge                13.51\n" +
                "^ Ww Beef Mince 132 Fat 750g              16.90\n" +
                "Ww Sliced Frozen Strawberries 500g         8.29\n" +
                "^ Tip-Top Trumpet Triple Choco4Pk 440ml    6.00\n" +
                "^ Tip Top Trumpet S/Berry Shake4x110ml     6.00\n" +
                "^ Macro Organic Basil Leaves 20g           2.50\n" +
                "* Ww Popcorn Bag Sweet N Salty 85g         1.20\n" +
                "ACT II Microwave Kettle Corn 85g           2.00\n" +
                "* Anchor Milk C/yum Strawberry 250ml       1.00\n" +
                "^ Greggs Mild AmericanMustard 250g         3.55\n" +
                "Broccoli Each                              2.29\n" +
                "^ Eta Ripples Ready Salted 150g            1.99\n" +
                "#SALVATION ARMY DONATION                   0.01\n" +
                "\n" +
                "21 SUBTOTAL                               $95.00\n" +
                "TOTAL                                     $95.00\n" +
                "\n" +
                "WOOLWORTHS NZ 9128                        GREENLANE\n" +
                "MERCH ID:611000069009128   TERM ID:       N9128062\n" +
                "Visa DEBIT\n" +
                "CARD: ............0428 T\n" +
                "A/C  A0000000031010        PURCHASE       NZ$95.00\n" +
                "TVR  000000000000";

        ParsedReceipt parsed = parser.parse(mockOcrText);
        List<ReceiptItem> items = parsed.getItems();

        assertNotNull(items);
        assertEquals(20, items.size());
        assertEquals(Long.valueOf(9500), parsed.getPrintedTotalCents());

        // 1. Carrot (Weighted)
        ReceiptItem carrot = items.get(0);
        assertEquals("Carrot", carrot.getName());
        assertEquals(0.520, carrot.getQuantity(), 0.001);
        assertEquals("kg", carrot.getUnit());
        assertEquals(195, carrot.getUnitPriceCents());
        assertEquals(101, carrot.getOriginalSubtotalCents());

        // 2. Canola Oil
        ReceiptItem oil = items.get(1);
        assertEquals("Ww Canola Oil 2L", oil.getName());
        assertEquals(799, oil.getOriginalSubtotalCents());
        assertFalse("Oil should not be special", oil.getSpecialMk());

        // 3. Keri Apple Orange Mango (Special ^)
        ReceiptItem keri1 = items.get(2);
        assertEquals("Keri Apple Orange Mango Drink 1L", keri1.getName());
        assertTrue("Keri 1 should be special", keri1.getSpecialMk());

        // 4. Keri Pulpy Orange (Multi-buy + Special ^ on previous line)
        ReceiptItem keri2 = items.get(3);
        assertEquals("Keri Pulpy Orange Drink 1L", keri2.getName());
        assertEquals(2.0, keri2.getQuantity(), 0.001);
        assertEquals(199, keri2.getUnitPriceCents());
        assertEquals(398, keri2.getOriginalSubtotalCents());
        assertTrue("Keri 2 should be special", keri2.getSpecialMk());

        // 8. Beef Mince (Special ^)
        ReceiptItem beef = items.get(8);
        assertTrue(beef.getName().contains("Beef Mince"));
        assertTrue("Beef Mince should be special", beef.getSpecialMk());

        // 20. Donation
        ReceiptItem donation = items.get(19);
        assertEquals("SALVATION ARMY DONATION", donation.getName());
        assertEquals(1, donation.getOriginalSubtotalCents());
        assertFalse("Donation should not be special", donation.getSpecialMk());
    }

    @Test
    public void testParseEmptyOrGarbageText() {
        String garbageText = "WELCOME TO WOOLWORTHS\n\nDUPLICATE RECEIPT\nTHANK YOU FOR SHOPPING\n";
        List<ReceiptItem> items = parser.parse(garbageText).getItems();

        assertNotNull(items);
        assertTrue(items.isEmpty());
    }
}
