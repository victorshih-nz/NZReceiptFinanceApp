package com.example.nzreceiptapp.domain.logic;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;

import java.util.List;

/**
 * Applies the validation rules shared by initial receipt saves and updates.
 * This class reports validation outcomes without changing the receipt.
 */
public final class ReceiptValidator {

    public ValidationResult validate(Receipt receipt) {
        if (receipt == null) {
            return invalid(ErrorCode.RECEIPT_REQUIRED);
        }

        if (receipt.getStore() == null
                || receipt.getStore().getNormalizedChainName().isEmpty()) {
            return invalid(ErrorCode.CHAIN_REQUIRED);
        }

        List<ReceiptItem> items = receipt.getItems();
        if (items == null || items.isEmpty()) {
            return invalid(ErrorCode.ITEMS_REQUIRED);
        }

        for (int index = 0; index < items.size(); index++) {
            ReceiptItem item = items.get(index);
            ValidationResult itemResult = validateItem(item, index);
            if (!itemResult.isValid()) {
                return itemResult;
            }
        }

        if (receipt.getFinalPayableCents() < 0) {
            return invalid(ErrorCode.RECEIPT_FINAL_TOTAL_NEGATIVE);
        }

        return valid();
    }

    private ValidationResult validateItem(ReceiptItem item, int itemIndex) {
        if (item == null) {
            return invalid(ErrorCode.ITEM_REQUIRED, itemIndex);
        }
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            return invalid(ErrorCode.ITEM_NAME_REQUIRED, itemIndex);
        }
        if (!(item.getQuantity() > 0)) {
            return invalid(ErrorCode.ITEM_QUANTITY_INVALID, itemIndex);
        }
        if (item.getUnitPriceCents() < 0) {
            return invalid(ErrorCode.ITEM_UNIT_PRICE_NEGATIVE, itemIndex);
        }

        Category category = item.getCategory();
        if (category != null && !category.isSubCategory()) {
            return invalid(ErrorCode.ITEM_CATEGORY_INVALID, itemIndex);
        }
        if (item.getFinalSubtotalCents() < 0) {
            return invalid(ErrorCode.ITEM_FINAL_SUBTOTAL_NEGATIVE, itemIndex);
        }

        return valid();
    }

    private static ValidationResult valid() {
        return new ValidationResult(null, ValidationResult.NO_ITEM_INDEX);
    }

    private static ValidationResult invalid(ErrorCode errorCode) {
        return invalid(errorCode, ValidationResult.NO_ITEM_INDEX);
    }

    private static ValidationResult invalid(ErrorCode errorCode, int itemIndex) {
        return new ValidationResult(errorCode, itemIndex);
    }

    public enum ErrorCode {
        RECEIPT_REQUIRED,
        CHAIN_REQUIRED,
        ITEMS_REQUIRED,
        ITEM_REQUIRED,
        ITEM_NAME_REQUIRED,
        ITEM_QUANTITY_INVALID,
        ITEM_UNIT_PRICE_NEGATIVE,
        ITEM_CATEGORY_INVALID,
        ITEM_FINAL_SUBTOTAL_NEGATIVE,
        RECEIPT_FINAL_TOTAL_NEGATIVE
    }

    public static final class ValidationResult {
        public static final int NO_ITEM_INDEX = -1;

        private final ErrorCode errorCode;
        private final int itemIndex;

        private ValidationResult(ErrorCode errorCode, int itemIndex) {
            this.errorCode = errorCode;
            this.itemIndex = itemIndex;
        }

        public boolean isValid() {
            return errorCode == null;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public int getItemIndex() {
            return itemIndex;
        }
    }
}
