package com.example.nzreceiptapp.data.parser;

import static org.junit.Assert.assertEquals;

import com.example.nzreceiptapp.domain.model.ReceiptItem;
import java.util.List;
import org.junit.Test;

/**
 * 專門測試 Woolworths 收據解析邏輯的單元測試
 */
public class WoolworthsParserTest {

    @Test
    public void testParseRawText_Success() {
        WoolworthsParser parser = new WoolworthsParser();
        
        // 模擬一段真實從 Woolworths 收據 OCR 出來的純文字
        String mockOcrText = "MUNCHEROS CHIPS      3.00\n" +
                             "WATTIES BAKED BEAN   3.00\n" +
                             "2 @ 1.50\n" +
                             "TOTAL                6.00";
        
        // 執行解析
        List<ReceiptItem> items = parser.parseRawText(mockOcrText);
        
        // 驗證 1：確認總共解析出 2 個商品品項
        assertEquals(2, items.size());
        
        // 驗證 2：檢查第一項商品
        ReceiptItem item1 = items.get(0);
        assertEquals("MUNCHEROS CHIPS", item1.getRawName());
        assertEquals("Muncheros Chips", item1.getCleanName());
        assertEquals(1.0, item1.getQuantity(), 0.0);
        assertEquals(300L, item1.getUnitPriceCents());
        assertEquals(300L, item1.getSubtotalCents());
        
        // 驗證 3：檢查第二項商品（多件購買 2 @ 1.50）
        ReceiptItem item2 = items.get(1);
        assertEquals("WATTIES BAKED BEAN", item2.getRawName());
        assertEquals("Watties Baked Bean", item2.getCleanName());
        assertEquals(2.0, item2.getQuantity(), 0.0);
        assertEquals(150L, item2.getUnitPriceCents());
        assertEquals(300L, item2.getSubtotalCents());
    }
}