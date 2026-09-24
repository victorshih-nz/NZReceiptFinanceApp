package com.example.nzreceiptapp.data.local.dao;

import static org.junit.Assert.*;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.nzreceiptapp.data.local.AppDatabase;
import com.example.nzreceiptapp.data.local.entity.ReceiptEntity;
import com.example.nzreceiptapp.data.local.entity.StoreEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;

@RunWith(AndroidJUnit4.class)
public class AnalyticsReceiptDaoTest {
    private AppDatabase database;
    private ReceiptDao dao;

    @Before public void setup() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                AppDatabase.class).allowMainThreadQueries().build();
        dao = database.receiptDao();
        dao.insertStore(new StoreEntity("s", "Woolworths", "Auckland"));
        insert("previous", LocalDateTime.of(2025, 12, 31, 23, 59));
        insert("first", LocalDateTime.of(2026, 1, 1, 0, 0));
        insert("last", LocalDateTime.of(2026, 1, 31, 23, 59, 59));
        insert("next", LocalDateTime.of(2026, 2, 1, 0, 0));
        insert("undated", null);
    }

    private void insert(String id, LocalDateTime date) {
        dao.insertReceipt(new ReceiptEntity(id, "s", date, 0, false));
    }

    @After public void tearDown() { database.close(); }

    @Test public void periodQueryUsesExclusiveEndAndExcludesNull() {
        assertEquals(2, dao.getReceiptsBetween(LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0)).size());
        assertEquals("first", dao.getReceiptsBetween(LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 2, 1, 0, 0)).get(0).receipt.id);
    }

    @Test public void datedQueryExcludesUnknownPurchaseDates() {
        assertEquals(4, dao.getDatedReceipts().size());
    }
}
