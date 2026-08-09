package com.example.nzreceiptapp.data.local;

import android.content.Context;

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

@Database(entities = {
        CategoryEntity.class, 
        CategoryRuleEntity.class,
        StoreEntity.class,
        ReceiptEntity.class,
        ReceiptItemEntity.class,
        ItemDiscountEntity.class
}, version = 2, exportSchema = true)
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

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "nz_receipt_db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
