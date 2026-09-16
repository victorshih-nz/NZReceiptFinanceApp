package com.example.nzreceiptapp.domain.usecase;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Answers whether a validated new Receipt matches the approved duplicate key.
 */
public final class CheckDuplicateReceiptUseCase {
    private final IReceiptRepository repository;

    public CheckDuplicateReceiptUseCase(IReceiptRepository repository) {
        this.repository = repository;
    }

    public boolean execute(Receipt draft) {
        if (draft == null || draft.getStore() == null) {
            throw new IllegalArgumentException("Receipt and Store are required");
        }
        String normalizedChain = draft.getStore().getNormalizedChainName();
        if (normalizedChain.isEmpty()) {
            throw new IllegalArgumentException("Normalized Chain is required");
        }
        LocalDateTime purchaseDate = draft.getPurchaseDate();
        if (purchaseDate == null) {
            throw new IllegalArgumentException("Purchase timestamp is required");
        }

        LocalDateTime hourStart = purchaseDate.withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime hourEnd = hourStart.plusHours(1);
        List<Receipt> candidates = repository.findDuplicateCandidates(
                normalizedChain, hourStart, hourEnd);

        long finalPayableCents = draft.getFinalPayableCents();
        for (Receipt candidate : candidates) {
            if (candidate.getFinalPayableCents() == finalPayableCents) {
                return true;
            }
        }
        return false;
    }
}
