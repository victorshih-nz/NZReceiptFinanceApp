package com.example.nzreceiptapp.data.repository;

import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.CategoryEntity;
import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemRow;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemWithDiscounts;
import com.example.nzreceiptapp.data.local.entity.ReceiptWithItems;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.Category;
import com.example.nzreceiptapp.domain.model.ItemDiscount;
import com.example.nzreceiptapp.domain.model.PageResult;
import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.ReceiptItemSummary;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;
import com.example.nzreceiptapp.domain.service.IReceiptImageStore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReceiptRepositoryImpl implements IReceiptRepository {
    private final ReceiptDao receiptDao;
    private final IReceiptImageStore imageStore;

    public ReceiptRepositoryImpl(ReceiptDao receiptDao) {
        this(receiptDao, new IReceiptImageStore() {
            @Override public String persist(String sourceUri) { return sourceUri; }
            @Override public void delete(String storedUri) { }
        });
    }

    public ReceiptRepositoryImpl(ReceiptDao receiptDao, IReceiptImageStore imageStore) {
        this.receiptDao = receiptDao;
        this.imageStore = imageStore;
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
                receipt.isSynced(),
                receipt.getRawOcrText(),
                receipt.getImageUri(),
                receipt.getPrintedTotalCents()
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

    @Override
    public List<Receipt> getAllReceipts() {
        return mapReceipts(receiptDao.getReceiptsPaged(Integer.MAX_VALUE, 0));
    }

    @Override
    public Receipt getReceiptById(String id) {
        ReceiptWithItems entity = receiptDao.getReceiptById(id);
        return entity != null ? mapToDomain(entity) : null;
    }

    @Override
    public PageResult<Receipt> getReceiptsPage(int pageNumber, int pageSize) {
        ReceiptDao.PageData<ReceiptWithItems> pageData =
                receiptDao.getReceiptsPage(pageNumber, pageSize);
        return new PageResult<>(
                mapReceipts(pageData.getRows()),
                pageData.getCurrentPage(),
                pageSize,
                pageData.getTotalRecords());
    }

    private List<Receipt> mapReceipts(List<ReceiptWithItems> entities) {
        List<Receipt> domainReceipts = new ArrayList<>();
        for (ReceiptWithItems entity : entities) {
            domainReceipts.add(mapToDomain(entity));
        }
        return domainReceipts;
    }

    @Override
    public PageResult<ReceiptItemSummary> getAllItemsPage(int pageNumber, int pageSize) {
        ReceiptDao.PageData<ReceiptItemRow> pageData =
                receiptDao.getAllItemsPage(pageNumber, pageSize);
        return new PageResult<>(
                mapItemSummaries(pageData.getRows()),
                pageData.getCurrentPage(),
                pageSize,
                pageData.getTotalRecords());
    }

    private List<ReceiptItemSummary> mapItemSummaries(List<ReceiptItemRow> entities) {
        List<ReceiptItemSummary> result = new ArrayList<>();
        for (ReceiptItemRow row : entities) {
            ReceiptItem item = mapItemToDomain(row.item, row.discounts, row.category);
            result.add(new ReceiptItemSummary(item, row.chainName, row.branchName, row.purchaseDate));
        }
        return result;
    }

    @Override
    public List<Receipt> findDuplicateCandidates(String normalizedChain,
                                                 LocalDateTime hourStart,
                                                 LocalDateTime hourEnd) {
        return mapReceipts(receiptDao.getReceiptsInPurchaseHour(
                normalizedChain, hourStart, hourEnd));
    }

    @Override
    public void deleteReceipt(String id) {
        ReceiptWithItems existing = receiptDao.getReceiptById(id);
        receiptDao.deleteReceiptAndUnusedStore(id);
        if (existing != null) imageStore.delete(existing.receipt.imageUri);
    }

    private ReceiptItem mapItemToDomain(ReceiptItemEntity itemEntity, List<ItemDiscountEntity> discountEntities, CategoryEntity categoryEntity) {
        List<ItemDiscount> discounts = new ArrayList<>();
        if (discountEntities != null) {
            for (ItemDiscountEntity discountEntity : discountEntities) {
                discounts.add(new ItemDiscount(
                        discountEntity.type,
                        discountEntity.description,
                        discountEntity.amountCents
                ));
            }
        }

        Category category = null;
        if (categoryEntity != null) {
            category = new Category(
                    categoryEntity.id,
                    categoryEntity.name,
                    null
            );
        }

        return new ReceiptItem(
                itemEntity.id,
                itemEntity.rawName,
                itemEntity.cleanedName,
                itemEntity.quantity,
                itemEntity.unit,
                itemEntity.unitPriceCents,
                discounts,
                category,
                itemEntity.specialMk
        );
    }

    private Receipt mapToDomain(ReceiptWithItems entity) {
        StoreEntity storeEntity = entity.store;
        Store store = new Store(storeEntity.id, storeEntity.chainName, storeEntity.branchName);

        List<ReceiptItem> items = new ArrayList<>();
        for (ReceiptItemWithDiscounts itemWithDiscounts : entity.items) {
            items.add(mapItemToDomain(itemWithDiscounts.item, itemWithDiscounts.discounts, itemWithDiscounts.category));
        }

        return new Receipt(
                entity.receipt.id,
                store,
                items,
                entity.receipt.purchaseDate,
                entity.receipt.totalDiscountCents,
                entity.receipt.isSynced,
                entity.receipt.rawOcrText,
                entity.receipt.imageUri,
                entity.receipt.printedTotalCents
        );
    }
}
