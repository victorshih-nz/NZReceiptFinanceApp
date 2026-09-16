package com.example.nzreceiptapp.domain.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.ItemDiscount;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReceiptValidatorTest {

    private static final Category PARENT_CATEGORY =
            new Category("food", "Food", null);
    private static final Category CHILD_CATEGORY =
            new Category("fruit", "Fruit", PARENT_CATEGORY);

    private ReceiptValidator validator;

    @Before
    public void setUp() {
        validator = new ReceiptValidator();
    }

    @Test
    public void validate_validReceipt_returnsValid() {
        ReceiptValidator.ValidationResult result = validator.validate(
                receipt("Woolworths", "Greenlane",
                        Collections.singletonList(validItem()), 0));

        assertTrue(result.isValid());
        assertNull(result.getErrorCode());
        assertEquals(ReceiptValidator.ValidationResult.NO_ITEM_INDEX,
                result.getItemIndex());
    }

    @Test
    public void validate_nullOrBlankBranch_returnsValid() {
        assertValid(receipt("Woolworths", null,
                Collections.singletonList(validItem()), 0));
        assertValid(receipt("Woolworths", "   ",
                Collections.singletonList(validItem()), 0));
    }

    @Test
    public void validate_chainWithSpacesAndPunctuation_returnsValidWhenNormalizedValueExists() {
        assertValid(receipt("  PAK'nSAVE  ", "Albany",
                Collections.singletonList(validItem()), 0));
    }

    @Test
    public void validate_nullReceipt_returnsReceiptRequired() {
        assertInvalid(null, ReceiptValidator.ErrorCode.RECEIPT_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
    }

    @Test
    public void validate_missingStore_returnsChainRequired() {
        Receipt receipt = new Receipt(
                "receipt-id", null, Collections.singletonList(validItem()),
                LocalDateTime.of(2026, 8, 17, 12, 0), 0, false);

        assertInvalid(receipt, ReceiptValidator.ErrorCode.CHAIN_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
    }

    @Test
    public void validate_nullBlankOrNormalizedEmptyChain_returnsChainRequired() {
        assertInvalid(receipt(null, "Branch", Collections.singletonList(validItem()), 0),
                ReceiptValidator.ErrorCode.CHAIN_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
        assertInvalid(receipt("   ", "Branch", Collections.singletonList(validItem()), 0),
                ReceiptValidator.ErrorCode.CHAIN_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
        assertInvalid(receipt(" !!! ", "Branch", Collections.singletonList(validItem()), 0),
                ReceiptValidator.ErrorCode.CHAIN_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
    }

    @Test
    public void validate_nullOrEmptyItems_returnsItemsRequired() {
        assertInvalid(receipt("Woolworths", "Branch", null, 0),
                ReceiptValidator.ErrorCode.ITEMS_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
        assertInvalid(receipt("Woolworths", "Branch", Collections.emptyList(), 0),
                ReceiptValidator.ErrorCode.ITEMS_REQUIRED,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
    }

    @Test
    public void validate_nullItem_returnsItemRequiredWithIndex() {
        assertInvalid(receipt("Woolworths", "Branch",
                        Arrays.asList(validItem(), null), 0),
                ReceiptValidator.ErrorCode.ITEM_REQUIRED, 1);
    }

    @Test
    public void validate_blankItemName_returnsItemNameRequiredWithIndex() {
        ReceiptItem item = item("   ", 1, 100, null, CHILD_CATEGORY, "EA");

        assertInvalid(receipt("Woolworths", "Branch",
                        Arrays.asList(validItem(), item), 0),
                ReceiptValidator.ErrorCode.ITEM_NAME_REQUIRED, 1);
    }

    @Test
    public void validate_zeroOrNegativeQuantity_returnsQuantityInvalid() {
        assertInvalid(receiptWithSingleItem(item(
                        "Apple", 0, 100, null, CHILD_CATEGORY, "EA")),
                ReceiptValidator.ErrorCode.ITEM_QUANTITY_INVALID, 0);
        assertInvalid(receiptWithSingleItem(item(
                        "Apple", -1, 100, null, CHILD_CATEGORY, "EA")),
                ReceiptValidator.ErrorCode.ITEM_QUANTITY_INVALID, 0);
    }

    @Test
    public void validate_negativeUnitPrice_returnsUnitPriceNegative() {
        assertInvalid(receiptWithSingleItem(item(
                        "Apple", 1, -1, null, CHILD_CATEGORY, "EA")),
                ReceiptValidator.ErrorCode.ITEM_UNIT_PRICE_NEGATIVE, 0);
    }

    @Test
    public void validate_optionalUnitAndZeroPrice_returnsValid() {
        assertValid(receiptWithSingleItem(item(
                "Free sample", 1, 0, null, CHILD_CATEGORY, null)));
        assertValid(receiptWithSingleItem(item(
                "Free sample", 1, 0, null, CHILD_CATEGORY, "")));
    }

    @Test
    public void validate_uncategorizedOrChildCategory_returnsValid() {
        assertValid(receiptWithSingleItem(item(
                "Apple", 1, 100, null, null, "EA")));
        assertValid(receiptWithSingleItem(item(
                "Apple", 1, 100, null, CHILD_CATEGORY, "EA")));
    }

    @Test
    public void validate_parentCategory_returnsCategoryInvalid() {
        assertInvalid(receiptWithSingleItem(item(
                        "Apple", 1, 100, null, PARENT_CATEGORY, "EA")),
                ReceiptValidator.ErrorCode.ITEM_CATEGORY_INVALID, 0);
    }

    @Test
    public void validate_negativeItemFinalSubtotal_returnsItemSubtotalNegative() {
        ItemDiscount discount = new ItemDiscount(
                ItemDiscount.DiscountType.UNKNOWN, "Large discount", 101);
        ReceiptItem item = item(
                "Apple", 1, 100, Collections.singletonList(discount),
                CHILD_CATEGORY, "EA");

        assertInvalid(receiptWithSingleItem(item),
                ReceiptValidator.ErrorCode.ITEM_FINAL_SUBTOTAL_NEGATIVE, 0);
    }

    @Test
    public void validate_negativeReceiptFinalPayable_returnsReceiptTotalNegative() {
        assertInvalid(receipt("Woolworths", "Branch",
                        Collections.singletonList(validItem()), 101),
                ReceiptValidator.ErrorCode.RECEIPT_FINAL_TOTAL_NEGATIVE,
                ReceiptValidator.ValidationResult.NO_ITEM_INDEX);
    }

    @Test
    public void validate_zeroItemAndReceiptFinalTotals_returnsValid() {
        ItemDiscount discount = new ItemDiscount(
                ItemDiscount.DiscountType.UNKNOWN, "Full discount", 100);
        ReceiptItem zeroTotalItem = item(
                "Apple", 1, 100, Collections.singletonList(discount),
                CHILD_CATEGORY, "EA");

        assertValid(receiptWithSingleItem(zeroTotalItem));
        assertValid(receipt("Woolworths", "Branch",
                Collections.singletonList(validItem()), 100));
    }

    private void assertValid(Receipt receipt) {
        assertTrue(validator.validate(receipt).isValid());
    }

    private void assertInvalid(Receipt receipt, ReceiptValidator.ErrorCode expectedCode,
                               int expectedItemIndex) {
        ReceiptValidator.ValidationResult result = validator.validate(receipt);

        assertEquals(expectedCode, result.getErrorCode());
        assertEquals(expectedItemIndex, result.getItemIndex());
    }

    private Receipt receiptWithSingleItem(ReceiptItem item) {
        return receipt("Woolworths", "Branch",
                Collections.singletonList(item), 0);
    }

    private Receipt receipt(String chain, String branch, List<ReceiptItem> items,
                            long totalDiscountCents) {
        return new Receipt(
                "receipt-id",
                new Store("store-id", chain, branch),
                items,
                LocalDateTime.of(2026, 8, 17, 12, 0),
                totalDiscountCents,
                false
        );
    }

    private ReceiptItem validItem() {
        return item("Apple", 1, 100, null, CHILD_CATEGORY, "EA");
    }

    private ReceiptItem item(String name, double quantity, long unitPriceCents,
                             List<ItemDiscount> discounts, Category category,
                             String unit) {
        return new ReceiptItem(
                "item-id",
                name,
                name,
                quantity,
                unit,
                unitPriceCents,
                discounts,
                category,
                false
        );
    }
}
