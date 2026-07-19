package com.example.nzreceiptapp.data.repository;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.ItemDiscount;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.util.ArrayList;
import java.util.List;

public class ReceiptRepositoryImpl implements IReceiptRepository {
    private final ReceiptDao receiptDao;

    public ReceiptRepositoryImpl(ReceiptDao receiptDao) {
        this.receiptDao = receiptDao;
    }

    @Override
    public void saveReceipt(Receipt receipt) {
        // 1. Map Store
        Store store = receipt.getStore();
        StoreEntity storeEntity = new StoreEntity(store.getId(), store.getChainName(), store.getBranchName());

        // 2. Map Receipt
        ReceiptEntity receiptEntity = new ReceiptEntity(
                receipt.getId(),
                store.getId(),
                receipt.getPurchaseDate(),
                receipt.getTotalDiscountCents(),
                receipt.isSynced()
        );

        // 3. Map Items and Discounts
        List<ReceiptItemEntity> itemEntities = new ArrayList<>();
        List<ItemDiscountEntity> discountEntities = new ArrayList<>();

        for (ReceiptItem item : receipt.getItems()) {
            String categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
            itemEntities.add(new ReceiptItemEntity(
                    item.getId(),
                    receipt.getId(),
                    item.getRawName(),
                    item.getCleanedName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getUnitPriceCents(),
                    categoryId,
                    item.getSpecialMk()
            ));

            if (item.getDiscounts() != null) {
                for (ItemDiscount discount : item.getDiscounts()) {
                    discountEntities.add(new ItemDiscountEntity(
                            item.getId(),
                            discount.getType(),
                            discount.getDescription(),
                            discount.getAmountCents()
                    ));
                }
            }
        }

        // 4. Save via Transaction
        receiptDao.saveFullReceipt(storeEntity, receiptEntity, itemEntities, discountEntities);
    }
}
