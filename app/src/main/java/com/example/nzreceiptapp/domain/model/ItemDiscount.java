package com.example.nzreceiptapp.domain.model;

/**
 * 品項專屬折扣 (Domain Value Object)
 */
public class ItemDiscount {
    public enum DiscountType {
        MEMBER_SAVING,    // Woolworths Everyday Rewards 會員折扣
        MULTI_BUY_PROMO,  // PAK'nSAVE / New World 多件組合優惠 (MULTIS)
        COUPON_DISCOUNT,  // 收據內嵌的條碼折抵
        UNKNOWN
    }

    private final DiscountType type;
    private final String description; // 收據上的原始折扣字樣
    private final long amountCents;   // 折扣金額，一律用「分」正值儲存

    public ItemDiscount(DiscountType type, String description, long amountCents) {
        this.type = type;
        this.description = description;
        this.amountCents = amountCents;
    }

    public DiscountType getType() { return type; }
    public String getDescription() { return description; }
    public long getAmountCents() { return amountCents; }
}
