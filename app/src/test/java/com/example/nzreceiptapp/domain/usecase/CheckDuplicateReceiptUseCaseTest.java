package com.example.nzreceiptapp.domain.usecase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.nzreceiptapp.domain.model.Receipt;
import com.example.nzreceiptapp.domain.model.ReceiptItem;
import com.example.nzreceiptapp.domain.model.Store;
import com.example.nzreceiptapp.domain.repository.IReceiptRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

public class CheckDuplicateReceiptUseCaseTest {
    @Mock private IReceiptRepository repository;

    private CheckDuplicateReceiptUseCase useCase;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new CheckDuplicateReceiptUseCase(repository);
    }

    @Test
    public void execute_matchingFinalTotalInSameHour_returnsTrue() {
        LocalDateTime purchaseDate = LocalDateTime.of(
                2026, 8, 17, 10, 59, 59, 900_000_000);
        Receipt draft = receipt("draft", " PAK'nSAVE! ", "Albany", purchaseDate, 9500);
        Receipt candidate = receipt(
                "saved", "PAKNSAVE", "Different Branch",
                LocalDateTime.of(2026, 8, 17, 10, 5, 12), 9500);
        LocalDateTime hourStart = LocalDateTime.of(2026, 8, 17, 10, 0);
        when(repository.findDuplicateCandidates(
                "paknsave", hourStart, hourStart.plusHours(1)))
                .thenReturn(Collections.singletonList(candidate));

        assertTrue(useCase.execute(draft));

        verify(repository).findDuplicateCandidates(
                "paknsave", hourStart, hourStart.plusHours(1));
    }

    @Test
    public void execute_differentFinalTotal_returnsFalse() {
        LocalDateTime purchaseDate = LocalDateTime.of(2026, 8, 17, 10, 5);
        Receipt draft = receipt("draft", "Woolworths", "A", purchaseDate, 9500);
        when(repository.findDuplicateCandidates(
                "woolworths", purchaseDate.withMinute(0),
                purchaseDate.withMinute(0).plusHours(1)))
                .thenReturn(Collections.singletonList(
                        receipt("saved", "Woolworths", "B", purchaseDate, 9499)));

        assertFalse(useCase.execute(draft));
    }

    @Test
    public void execute_noCandidate_returnsFalse() {
        LocalDateTime purchaseDate = LocalDateTime.of(2026, 8, 17, 11, 0);
        Receipt draft = receipt("draft", "Woolworths", "", purchaseDate, 9500);
        when(repository.findDuplicateCandidates(
                "woolworths", purchaseDate, purchaseDate.plusHours(1)))
                .thenReturn(Collections.emptyList());

        assertFalse(useCase.execute(draft));
    }

    @Test
    public void execute_missingReceipt_rejectsBeforeRepository() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));

        verify(repository, never()).findDuplicateCandidates(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test
    public void execute_normalizedEmptyChain_rejectsBeforeRepository() {
        Receipt draft = receipt(
                "draft", " !!! ", "", LocalDateTime.of(2026, 8, 17, 10, 0), 9500);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(draft));
    }

    @Test
    public void execute_missingPurchaseTimestamp_rejectsBeforeRepository() {
        Receipt draft = receipt("draft", "Woolworths", "", null, 9500);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(draft));
    }

    private Receipt receipt(String id,
                            String chain,
                            String branch,
                            LocalDateTime purchaseDate,
                            long totalCents) {
        ReceiptItem item = new ReceiptItem(
                "item-" + id, "Item", "Item", 1, "ea", totalCents,
                Collections.emptyList(), null, false);
        return new Receipt(
                id, new Store("store-" + id, chain, branch),
                Collections.singletonList(item), purchaseDate, 0, false);
    }
}
