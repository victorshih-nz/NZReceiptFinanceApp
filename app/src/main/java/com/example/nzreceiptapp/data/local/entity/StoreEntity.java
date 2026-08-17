package com.example.nzreceiptapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.nzreceiptapp.domain.model.Store;

@Entity(tableName = "stores",
        indices = {@Index(value = {"normalized_chain", "normalized_branch"},
                unique = true)})
public class StoreEntity {
    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "chain_name")
    public String chainName;

    @ColumnInfo(name = "branch_name")
    public String branchName;

    @ColumnInfo(name = "normalized_chain", defaultValue = "''")
    @NonNull
    public String normalizedChain;

    @ColumnInfo(name = "normalized_branch", defaultValue = "''")
    @NonNull
    public String normalizedBranch;

    @Ignore
    public StoreEntity(@NonNull String id, String chainName, String branchName) {
        this(id, chainName, branchName,
                new Store(id, chainName, branchName).getNormalizedChainName(),
                new Store(id, chainName, branchName).getNormalizedBranchName());
    }

    public StoreEntity(@NonNull String id,
                       String chainName,
                       String branchName,
                       @NonNull String normalizedChain,
                       @NonNull String normalizedBranch) {
        this.id = id;
        this.chainName = chainName;
        this.branchName = branchName;
        this.normalizedChain = normalizedChain;
        this.normalizedBranch = normalizedBranch;
    }
}
