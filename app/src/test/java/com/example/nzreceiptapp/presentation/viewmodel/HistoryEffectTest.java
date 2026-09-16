package com.example.nzreceiptapp.presentation.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HistoryEffectTest {

    @Test
    public void consume_returnsTypeOnlyOnce() {
        HistoryEffect effect = HistoryEffect.refreshFailed();

        assertEquals(HistoryEffect.Type.REFRESH_FAILED, effect.consume());
        assertNull(effect.consume());
    }
}
