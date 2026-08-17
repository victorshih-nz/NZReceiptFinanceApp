package com.example.nzreceiptapp.data.local;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

import com.example.nzreceiptapp.data.local.dao.CategoryDao;
import com.example.nzreceiptapp.data.local.dao.ReceiptDao;
import com.example.nzreceiptapp.data.local.entity.CategoryEntity;
import com.example.nzreceiptapp.data.local.entity.CategoryRuleEntity;
import com.example.nzreceiptapp.data.local.entity.ItemDiscountEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.ReceiptItemEntity;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;
import com.example.nzreceiptapp.domain.model.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Database(entities = {
        CategoryEntity.class, 
        CategoryRuleEntity.class,
        StoreEntity.class,
        ReceiptEntity.class,
        ReceiptItemEntity.class,
        ItemDiscountEntity.class
}, version = 3, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract CategoryDao categoryDao();
    public abstract ReceiptDao receiptDao();

    private static volatile AppDatabase INSTANCE;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE receipts ADD COLUMN raw_ocr_text TEXT");
            database.execSQL("ALTER TABLE receipts ADD COLUMN image_uri TEXT");
            database.execSQL("ALTER TABLE receipts ADD COLUMN printed_total_cents INTEGER");

            database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_parent_id "
                    + "ON categories(parent_id)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_category_rules_category_id "
                    + "ON category_rules(category_id)");

            // Point duplicate store references at one canonical row before adding
            // the unique chain/branch index.
            database.execSQL("UPDATE receipts SET store_id = ("
                    + "SELECT MIN(s2.id) FROM stores s1 JOIN stores s2 "
                    + "ON COALESCE(s1.chain_name, '') = COALESCE(s2.chain_name, '') "
                    + "AND COALESCE(s1.branch_name, '') = COALESCE(s2.branch_name, '') "
                    + "WHERE s1.id = receipts.store_id)");
            database.execSQL("DELETE FROM stores WHERE id NOT IN "
                    + "(SELECT DISTINCT store_id FROM receipts)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "index_stores_chain_name_branch_name "
                    + "ON stores(chain_name, branch_name)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE stores ADD COLUMN "
                    + "normalized_chain TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE stores ADD COLUMN "
                    + "normalized_branch TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE receipts ADD COLUMN "
                    + "saved_sequence INTEGER NOT NULL DEFAULT 0");

            Map<String, String> canonicalStoreIds = new HashMap<>();
            List<String> duplicateStoreIds = new ArrayList<>();
            try (Cursor cursor = database.query(
                    "SELECT id, chain_name, branch_name FROM stores ORDER BY rowid ASC")) {
                int idIndex = cursor.getColumnIndexOrThrow("id");
                int chainIndex = cursor.getColumnIndexOrThrow("chain_name");
                int branchIndex = cursor.getColumnIndexOrThrow("branch_name");
                while (cursor.moveToNext()) {
                    String storeId = cursor.getString(idIndex);
                    String chain = cursor.isNull(chainIndex)
                            ? null : cursor.getString(chainIndex);
                    String branch = cursor.isNull(branchIndex)
                            ? null : cursor.getString(branchIndex);
                    Store store = new Store(storeId, chain, branch);
                    String normalizedChain = store.getNormalizedChainName();
                    String normalizedBranch = store.getNormalizedBranchName();
                    database.execSQL(
                            "UPDATE stores SET normalized_chain = ?, "
                                    + "normalized_branch = ? WHERE id = ?",
                            new Object[]{normalizedChain, normalizedBranch, storeId});

                    String identityKey = normalizedChain + '\u0000' + normalizedBranch;
                    String canonicalId = canonicalStoreIds.get(identityKey);
                    if (canonicalId == null) {
                        canonicalStoreIds.put(identityKey, storeId);
                    } else {
                        database.execSQL(
                                "UPDATE receipts SET store_id = ? WHERE store_id = ?",
                                new Object[]{canonicalId, storeId});
                        duplicateStoreIds.add(storeId);
                    }
                }
            }

            for (String duplicateStoreId : duplicateStoreIds) {
                database.execSQL("DELETE FROM stores WHERE id = ?",
                        new Object[]{duplicateStoreId});
            }

            database.execSQL("DROP INDEX IF EXISTS "
                    + "index_stores_chain_name_branch_name");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                    + "index_stores_normalized_chain_normalized_branch "
                    + "ON stores(normalized_chain, normalized_branch)");
            database.execSQL("UPDATE receipts SET saved_sequence = ("
                    + "SELECT COUNT(*) FROM receipts AS earlier "
                    + "WHERE earlier.rowid <= receipts.rowid)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "nz_receipt_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
