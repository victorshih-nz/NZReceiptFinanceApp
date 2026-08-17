package com.example.nzreceiptapp.domain.repository;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.model.PageResult;

import java.time.LocalDateTime;
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
     * 取得包含總筆數與有效頁碼的完整發票分頁結果。
     */
    PageResult<Receipt> getReceiptsPage(int pageNumber, int pageSize);

    /**
     * 取得包含總筆數與有效頁碼的完整品項分頁結果。
     */
    PageResult<ReceiptItemSummary> getAllItemsPage(int pageNumber, int pageSize);

    /**
     * Returns receipts in the requested purchase hour whose normalized Chain
     * matches the supplied comparison key. The end timestamp is exclusive.
     */
    List<Receipt> findDuplicateCandidates(String normalizedChain,
                                          LocalDateTime hourStart,
                                          LocalDateTime hourEnd);

    /**
     * 根據 ID 刪除發票
     */
    void deleteReceipt(String id);
}
