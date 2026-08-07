package com.example.nzreceiptapp.domain.service;

/** Ensures bundled category data is available before parsing starts. */
public interface ICategoryInitializer {
    void ensureInitialized();
}
