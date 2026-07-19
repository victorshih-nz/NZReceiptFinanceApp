package com.example.nzreceiptapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stores")
public class StoreEntity {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "chain_name")
    public String chainName;

    @ColumnInfo(name = "branch_name")
    public String branchName;

    public StoreEntity(@NonNull String id, String chainName, String branchName) {
        this.id = id;
        this.chainName = chainName;
        this.branchName = branchName;
    }
}
