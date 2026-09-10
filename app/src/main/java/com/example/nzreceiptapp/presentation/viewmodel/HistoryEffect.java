package com.example.nzreceiptapp.presentation.viewmodel;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A one-time History event. The same LiveData value may be delivered again
 * after view recreation, but its effect can only be consumed once.
 */
public final class HistoryEffect {

    public enum Type {
        REFRESH_FAILED,
        PAGE_LOAD_FAILED
    }

    private final Type type;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    private HistoryEffect(Type type) {
        this.type = type;
    }

    public static HistoryEffect refreshFailed() {
        return new HistoryEffect(Type.REFRESH_FAILED);
    }

    public static HistoryEffect pageLoadFailed() {
        return new HistoryEffect(Type.PAGE_LOAD_FAILED);
    }

    /** Returns the effect once, then returns {@code null} on every replay. */
    public Type consume() {
        return consumed.compareAndSet(false, true) ? type : null;
    }
}
