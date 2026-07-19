package com.example.nzreceiptapp.domain.repository;

import com.example.nzreceiptapp.domain.model.Receipt;

/**
 * 倉儲抽象介面 (Domain Layer)
 */
public interface IReceiptRepository {
    /**
     * 儲存整張發票，包含其關聯的店家、品項與折扣
     */
    void saveReceipt(Receipt receipt);
}
