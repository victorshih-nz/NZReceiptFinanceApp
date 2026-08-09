package com.example.nzreceiptapp.domain.service;

/** Persists receipt images outside temporary cache storage. */
public interface IReceiptImageStore {
    String persist(String sourceUri);
    void delete(String storedUri);
}
