package com.example.nzreceiptapp.domain.service;

/**
 * OCR 服務抽象介面 (Domain Layer)
 */
public interface IOCRService {

    interface OnOCRCompleteListener {
        void onSuccess(String text);
        void onFailure(Exception e);
    }

    /**
     * 從指定路徑的圖片中擷取文字
     * @param imagePath 圖片的本地路徑或 URI 字串
     * @param listener 回調介面
     */
    void extractText(String imagePath, OnOCRCompleteListener listener);
}
