package com.example.nzreceiptapp.domain.repository;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import java.util.List;

/**
 * 倉儲抽象介面 (Domain Layer)
 */
public interface IReceiptRepository {
    /**
     * 儲存整張發票，包含其關聯的店家、品項與折扣
     */
    void saveReceipt(Receipt receipt);

    /**
     * 獲取所有已儲存的發票清單
     */
    List<Receipt> getAllReceipts();

    /**
     * 根據 ID 獲取單張發票
     */
    Receipt getReceiptById(String id);

    /**
     * 分頁獲取發票清單
     * @param limit 每頁數量
     * @param offset 偏移量 (page * limit)
     */
    List<Receipt> getReceiptsPaged(int limit, int offset);

    /**
     * 分頁獲取扁平化的所有品項清單
     */
    List<ReceiptItemSummary> getAllItemsPaged(int limit, int offset);

    /**
     * 根據 ID 刪除發票
     */
    void deleteReceipt(String id);
}
