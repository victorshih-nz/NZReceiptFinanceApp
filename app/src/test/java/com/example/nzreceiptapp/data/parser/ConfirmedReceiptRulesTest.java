package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.*;
import com.example.nzreceiptapp.domain.model.ParsedReceipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import org.junit.Test;

public class ConfirmedReceiptRulesTest {
    @Test public void woolworthsFirstLineTotalSecondLineWeight() {
        ParsedReceipt result = new WoolworthsParser().parse("Onion Brown Loose 2.47\n0.705 kg NET @ $3.50/kg\nTOTAL 2.47");
        assertEquals(1, result.getItems().size());
        ReceiptItem onion = result.getItems().get(0);
        assertEquals("Onion Brown Loose", onion.getName());
        assertEquals(0.705, onion.getQuantity(), 0.0001);
        assertEquals("kg", onion.getUnit());
        assertEquals(350, onion.getUnitPriceCents());
        assertEquals(247, onion.getFinalSubtotalCents());
    }
    @Test public void woolworthsMultiQtyOnFollowingLine() {
        ParsedReceipt result = new WoolworthsParser().parse("Pringles Crisps Original 134g\nQty 3 @ $2.99 each 8.97\nTOTAL 8.97");
        assertEquals(1, result.getItems().size());
        ReceiptItem item = result.getItems().get(0);
        assertEquals(3.0, item.getQuantity(), 0.0001);
        assertEquals("ea", item.getUnit());
        assertEquals(299, item.getUnitPriceCents());
        assertEquals(897, item.getOriginalSubtotalCents());
    }
    @Test public void pakMultiAndPfDiscount() {
        ParsedReceipt result = new PakNSaveParser().parse("JOHN WEST TUNA OIL BLEND 185G\n2 @ $3.80 $7.60\nPF_JOHN WEST TUNA -$1.60\nTOTAL DUE 6.00");
        assertEquals(1, result.getItems().size());
        ReceiptItem item = result.getItems().get(0);
        assertEquals(2, item.getQuantity(), 0.0001);
        assertEquals("ea", item.getUnit());
        assertEquals(760, item.getOriginalSubtotalCents());
        assertEquals(1, item.getDiscounts().size());
        assertEquals(160, item.getDiscounts().get(0).getAmountCents());
        assertEquals(600, item.getFinalSubtotalCents());
    }
    @Test public void pakWeightedAndNoPfNoDiscount() {
        ParsedReceipt result = new PakNSaveParser().parse("BANANAS LOOSE\n0.645 kg @ $3.49 /kg 2.25\nPAMS MILK 2L 3.89\nTOTAL 6.14");
        assertEquals(2, result.getItems().size());
        assertEquals("kg", result.getItems().get(0).getUnit());
        assertEquals(0.645, result.getItems().get(0).getQuantity(), 0.0001);
        assertTrue(result.getItems().get(1).getDiscounts().isEmpty());
    }
    @Test public void unmatchedPfNeverBecomesProductOrWrongDiscount() {
        ParsedReceipt result = new PakNSaveParser().parse("PAMS MILK 2L 3.89\nPF_DIFFERENT PRODUCT -$1.00\nTOTAL 2.89");
        assertEquals(1, result.getItems().size());
        assertTrue(result.getItems().get(0).getDiscounts().isEmpty());
        assertNotEquals(result.getPrintedTotalCents().longValue(), result.getItems().get(0).getFinalSubtotalCents());
    }
}
