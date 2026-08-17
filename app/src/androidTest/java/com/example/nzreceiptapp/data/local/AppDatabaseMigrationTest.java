package com.example.nzreceiptapp.data.local;

import static org.junit.Assert.assertEquals;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {
    private static final String TEST_DATABASE = "history-migration-test";
    private static final String EMPTY_BRANCH_DATABASE =
            "history-empty-branch-migration-test";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class.getCanonicalName(),
            new FrameworkSQLiteOpenHelperFactory());

    @Test
    public void migrate2To3_normalizesStoresAndPreservesReceiptOrder() throws IOException {
        SupportSQLiteDatabase database = helper.createDatabase(TEST_DATABASE, 2);
        database.execSQL("INSERT INTO stores(id, chain_name, branch_name) "
                + "VALUES ('store-a', 'Woolworths', ' Greenlane ')");
        database.execSQL("INSERT INTO stores(id, chain_name, branch_name) "
                + "VALUES ('store-b', ' wool-worths! ', 'GREEN LANE!')");
        insertReceipt(database, "receipt-a", "store-a", "2026-08-17T10:05:12");
        insertReceipt(database, "receipt-b", "store-b", "2026-08-17T10:05:12");
        database.close();

        database = helper.runMigrationsAndValidate(
                TEST_DATABASE, 3, true, AppDatabase.MIGRATION_2_3);

        try (Cursor stores = database.query(
                "SELECT id, normalized_chain, normalized_branch FROM stores")) {
            assertEquals(1, stores.getCount());
            stores.moveToFirst();
            assertEquals("store-a", stores.getString(0));
            assertEquals("woolworths", stores.getString(1));
            assertEquals("greenlane", stores.getString(2));
        }
        try (Cursor receipts = database.query(
                "SELECT store_id, saved_sequence FROM receipts "
                        + "ORDER BY saved_sequence ASC")) {
            assertEquals(2, receipts.getCount());
            receipts.moveToFirst();
            assertEquals("store-a", receipts.getString(0));
            assertEquals(1, receipts.getLong(1));
            receipts.moveToNext();
            assertEquals("store-a", receipts.getString(0));
            assertEquals(2, receipts.getLong(1));
        }
    }

    @Test
    public void migrate2To3_collapsesEquivalentEmptyBranches() throws IOException {
        SupportSQLiteDatabase database =
                helper.createDatabase(EMPTY_BRANCH_DATABASE, 2);
        database.execSQL("INSERT INTO stores(id, chain_name, branch_name) "
                + "VALUES ('store-a', 'PAK''nSAVE', NULL)");
        database.execSQL("INSERT INTO stores(id, chain_name, branch_name) "
                + "VALUES ('store-b', ' pak n save ', '   ')");
        database.execSQL("INSERT INTO stores(id, chain_name, branch_name) "
                + "VALUES ('store-c', 'PAK-N-SAVE', '!!!')");
        insertReceipt(database, "receipt-a", "store-a", "2026-08-17T12:00:00");
        insertReceipt(database, "receipt-b", "store-b", "2026-08-17T11:00:00");
        insertReceipt(database, "receipt-c", "store-c", "2026-08-17T10:00:00");
        database.close();

        database = helper.runMigrationsAndValidate(
                EMPTY_BRANCH_DATABASE, 3, true, AppDatabase.MIGRATION_2_3);

        try (Cursor stores = database.query(
                "SELECT id, normalized_chain, normalized_branch FROM stores")) {
            assertEquals(1, stores.getCount());
            stores.moveToFirst();
            assertEquals("store-a", stores.getString(0));
            assertEquals("paknsave", stores.getString(1));
            assertEquals("", stores.getString(2));
        }
        try (Cursor receipts = database.query(
                "SELECT store_id, saved_sequence FROM receipts "
                        + "ORDER BY saved_sequence ASC")) {
            assertEquals(3, receipts.getCount());
            for (long expectedSequence = 1; expectedSequence <= 3;
                 expectedSequence++) {
                receipts.moveToNext();
                assertEquals("store-a", receipts.getString(0));
                assertEquals(expectedSequence, receipts.getLong(1));
            }
        }
    }

    private void insertReceipt(SupportSQLiteDatabase database,
                               String receiptId,
                               String storeId,
                               String purchaseDate) {
        database.execSQL("INSERT INTO receipts("
                        + "id, store_id, purchase_date, total_discount_cents, "
                        + "is_synced, raw_ocr_text, image_uri, printed_total_cents) "
                        + "VALUES (?, ?, ?, 0, 0, 'OCR', 'image.jpg', 100)",
                new Object[]{receiptId, storeId, purchaseDate});
    }
}
