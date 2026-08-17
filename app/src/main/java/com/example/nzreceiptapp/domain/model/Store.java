package com.example.nzreceiptapp.domain.model;

import java.util.Locale;

/**
 * 店家與分店實體 (Domain Entity)
 */
public class Store {
    private final String id;          // UUID 格式
    private final String chainName;   // 系統識別鎖定："Woolworths", "PAK'nSAVE", "New World", "Four Square"
    private final String branchName;  // 分店名稱，例如："Albany", "Grey Lynn"

    public Store(String id, String chainName, String branchName) {
        this.id = id;
        this.chainName = chainName;
        this.branchName = branchName;
    }

    public String getId() { return id; }
    public String getChainName() { return chainName; }
    public String getBranchName() { return branchName; }

    public String getNormalizedChainName() {
        return normalize(chainName);
    }

    public String getNormalizedBranchName() {
        return normalize(branchName);
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim()
                        .replaceAll("[^A-Za-z0-9]", "")
                        .toLowerCase(Locale.ROOT);
    }
}
