# NZReceiptFinanceApp — History Feature Design

**Document ID:** HFD-001  
**Version:** 1.0 — Approved / Frozen  
**Architecture:** MVVM + Clean Architecture

## 1. Purpose

This document defines the target behaviour and architecture for:

1. Receipt History — browse and delete persisted receipts.
2. Receipt Items — open one persisted receipt and view/add/edit/delete its items.
3. Parsed Receipt Preview — review a parsed Draft Receipt before first persistence.

Parsed Receipt Preview belongs to the Scan / Receipt Review flow, not History. Save persists the Draft and navigates to History; Discard returns to Scan without a database write.

## 2. Scope

### In Scope

- Receipt History list ordered by purchase date/time descending.
- Receipt pagination: default 30, selectable 15/30/50, direct page selection, Previous/Next.
- Receipt delete with confirmation and paging recovery.
- Open one Receipt and display only that Receipt's Items.
- Receipt Items pagination: default 30, selectable 15/30/50.
- Persisted Items read-only by default.
- Per-item Edit → Save workflow.
- Add Item using the same editable row UI as Edit Item.
- Delete persisted Item.
- Recalculate Calculated Total after item add/edit/delete.
- Preserve Printed Total separately.
- Large modal Parsed Receipt Preview.
- Draft Item add/edit/delete before first Save.
- Preview Save / Discard.
- Receipt date/time parsed from receipt where available, otherwise parse-time fallback.

### Out of Scope

- Global All Items list.
- Manual Add Receipt from History.
- Search, filters, date range, custom sorting.
- Bulk delete / undo.
- Cloud sync changes.
- Export / analytics changes.
- New Room schema solely for this feature.
- Separate editable Raw OCR Item Name.

## 3. Terminology and Totals

### Draft Receipt

A Receipt domain object created after OCR/Parse but not yet persisted.

### Persisted Receipt

A Receipt successfully written to the local database.

### Printed Total

The total printed on the physical receipt and recognised by parser/OCR. It is reference data and is not overwritten by later item edits.

### Calculated Total

Calculated from the current Receipt Items using existing domain logic, currently represented by `Receipt.getFinalPayableCents()`.

## 4. High-Level Scan Flow

```text
Scan
→ Confirm image
→ OCR
→ Parse
→ Create Draft Receipt in memory
→ Large Preview Modal

Discard
→ clear Draft
→ no DB write
→ close Preview
→ return to Scan

Save
→ validate Draft
→ SaveReceiptUseCase
→ persist Receipt + Items
→ close Preview
→ navigate History
→ refresh Receipt list
```

A failed Preview Save keeps the Preview open and preserves the Draft for correction/retry.

## 5. Receipt Date/Time Rule

**BR-DATE-01**

1. If date/time can be parsed from the receipt, use it as `purchaseDate`.
2. Otherwise use local parse-time date/time.
3. Assign the fallback before Draft Preview.

History ordering uses `purchaseDate`, not save time.

## 6. History Receipt List

### Default Behaviour

- History opens to Receipt list.
- Newer purchase date/time appears first.
- Existing saved sequence may be used as deterministic secondary ordering where timestamps are equal.

### Receipt Row

Conceptual UI:

```text
[ Receipt ] 🗑
```

Tap Receipt → open that Receipt's Items.  
Tap delete → confirmation required.

### Receipt Pagination

```text
←   [2 ▼] / 6   →   [30 ▼]
```

Rules:

- default page size 30;
- options 15/30/50;
- direct page selection;
- Previous/Next;
- page-size change returns to page 1.

### Receipt Delete

```text
Delete Receipt
→ confirmation
→ Cancel: no change
→ Confirm: DeleteReceiptUseCase
→ repository/Room delete
→ image cleanup according to repository behaviour
→ recalculate paging
→ keep current page if valid
→ otherwise new last page
```

## 7. Receipt Items

Selecting a Receipt displays only Items belonging to that Receipt. There is no global All Items mode.

### Default Row State

Persisted Items are read-only by default:

```text
Milk   2   ea   $4.50   Dairy   ✎ 🗑
```

### Editable Fields

- Item Name (cleaned/display name)
- Quantity
- Unit
- Unit Price
- Category
- Discount information

Raw OCR name remains internal reference data.

### Edit Item

```text
Read-only row ✎
→ tap ✎
→ selected row only becomes editable
→ ✎ becomes 💾
→ edit fields
→ tap 💾
→ validate
→ update Receipt aggregate via UpdateReceiptUseCase
→ recalculate Calculated Total
→ preserve Printed Total
→ refresh Receipt
→ row becomes read-only
→ 💾 becomes ✎
```

Validation/update failure keeps the row in edit mode and does not represent the update as successful.

There is no whole-page Save/Discard for persisted Receipt Items.

### Delete Item

```text
Tap 🗑
→ remove selected Item from Receipt aggregate
→ UpdateReceiptUseCase
→ recalculate Calculated Total
→ preserve Printed Total
→ recalculate pages
→ keep current page if valid
→ otherwise show new last page
```

No separate Item-delete confirmation is required in v1.0.

## 8. Add Item to Existing Receipt

Add Item reuses the same editable row UI as Edit Item.

```text
Tap green +
→ create new unsaved editable Item row
→ same fields as Edit Item
→ show 💾
→ enter values
→ tap 💾
→ validate
→ persist updated Receipt aggregate
→ recalculate Calculated Total
→ preserve Printed Total
→ row becomes read-only
→ 💾 becomes ✎
```

Pressing `+` alone performs no database write.

## 9. Receipt Items Pagination

- default 30;
- options 15/30/50;
- direct page selection;
- Previous/Next;
- `x / total` display.

Receipt History remains database-paged. Items for one selected Receipt may be paged in presentation/ViewModel state after loading the Receipt aggregate.

## 10. Total Model

```text
Printed Total     = physical receipt reference value
Calculated Total  = current domain calculation from current Items
```

Example: Printed Total remains $17 after deleting a $4 Item, while Calculated Total becomes $13.

## 11. Parsed Receipt Preview

### Ownership / Presentation

- belongs to Scan flow;
- large modal using most available screen area;
- not a normal full-screen destination;
- implementation may use `DialogFragment` or equivalent modal component.

### Draft State

Before Save:

- no Receipt row for the Draft in Room;
- no Draft Items in Room;
- edits/add/delete affect Draft memory only;
- Calculated Total updates from Draft Items.

### Preview Controls

- editable receipt/item review;
- Add Item;
- Delete Item;
- Item paging 15/30/50, default 30;
- Save;
- Discard.

### Save

```text
Preview
→ Save
→ validate
→ SaveReceiptUseCase
→ repository
→ Room

Success:
close Preview
→ navigate History
→ refresh History

Failure:
keep Preview open
→ keep Draft
→ show error
```

### Discard

```text
Preview
→ Discard
→ discard entire Draft Receipt
→ no DB write
→ close Preview
→ return to Scan
```

## 12. Use Cases

- **UC-HIS-01 View Receipt History**
- **UC-HIS-02 View Receipt Items**
- **UC-HIS-03 Edit Receipt Item**
- **UC-HIS-04 Add Receipt Item**
- **UC-HIS-05 Delete Receipt Item**
- **UC-HIS-06 Delete Receipt**
- **UC-SCAN-REVIEW-01 Review Parsed Receipt**

Use-case relationship:

```text
User ── View Receipt History
User ── View Receipt Items
View Receipt Items <<extend>> View Receipt History
```

## 13. Target Architecture

```text
Presentation → Domain ← Data
                    ↑
                  DI root
```

Key flows:

```text
HistoryFragment
→ HistoryViewModel
→ GetReceiptsPagedUseCase / DeleteReceiptUseCase
→ IReceiptRepository

Receipt Items View
→ ReceiptDetailViewModel or ReceiptItemsViewModel
→ GetReceiptByIdUseCase / UpdateReceiptUseCase
→ IReceiptRepository

Scanner / Preview Modal
→ ScannerViewModel
→ ParseReceiptUseCase / SaveReceiptUseCase
→ IReceiptRepository
```

No presentation component may access Room directly.

## 14. Aggregate Update Decision

For persisted Item add/edit/delete, reuse Receipt as the aggregate boundary:

```text
Load Receipt
→ modify ReceiptItem collection
→ UpdateReceiptUseCase
→ IReceiptRepository.updateReceipt()
→ ReceiptRepositoryImpl
→ ReceiptDao.updateFullReceipt()
```

Do not add independent item persistence APIs unless implementation evidence proves this path inadequate and SA approves the change.

## 15. UI State Responsibilities

### HistoryUiState

- Receipt list
- load state
- Receipt paging state
- error state

Remove global All Items state.

### Receipt Items State

- loaded Receipt
- all Items / visible Item page
- page size / page count
- editing Item state
- new unsaved Item state
- loading/saving state
- validation/error state
- Printed Total
- Calculated Total

### Scanner Preview State

Scanner state owns Draft Receipt until Save or Discard. Preview must not create a persisted History record before Save succeeds.

## 16. Data Integrity Rules

- **BR-DATA-01:** Draft Receipt is not stored before Preview Save.
- **BR-DATA-02:** Discard performs no DB write.
- **BR-DATA-03:** Persisted Item edits write only after Item Save succeeds.
- **BR-DATA-04:** Printed Total is preserved.
- **BR-DATA-05:** Calculated Total reflects current Items.
- **BR-DATA-06:** Failed save/update is never shown as success.
- **BR-DATA-07:** Failed Preview Save preserves Draft.
- **BR-DATA-08:** Receipt/Item deletion recalculates valid paging.

## 17. Current Reference Implementation Gaps

Target implementation must address:

1. remove global All Items History mode;
2. Receipt default page size 30;
3. Receipt delete confirmation;
4. persisted Items read-only with per-item Edit/Save;
5. persisted Item add/delete;
6. reuse `UpdateReceiptUseCase` for persisted Item changes;
7. approved editable discount data;
8. Preview becomes large modal;
9. Preview item paging;
10. Preview Save → refreshed History;
11. Preview Discard → Scan;
12. Preview Save failure preserves Draft.

## 18. Acceptance Criteria

### History

- Receipt list default view and newest purchase first.
- page size 30; options 15/30/50.
- direct page selection and Previous/Next.
- Receipt delete confirmation.
- valid page recovery after deletion.
- no global All Items mode.

### Receipt Items

- only selected Receipt Items shown.
- read-only by default with ✎ and 🗑.
- selected row only enters edit mode.
- ✎ → 💾 → ✎ around successful edit.
- approved fields editable; Raw OCR Name not separately editable.
- green `+` creates unsaved editable row using same Edit Item UI.
- `+` alone does not persist; 💾 does.
- persisted Item delete supported.
- page size 30; options 15/30/50; direct page selection and Previous/Next.
- deleting last Item on last page recovers to new last page.
- no whole-page Save/Discard for persisted Receipt Items.

### Totals

- Printed Total preserved.
- Calculated Total updates after add/edit/delete.

### Preview

- Draft remains non-persisted before Save.
- large modal, not full-screen destination.
- Draft Items editable/addable/deletable.
- Preview paging default 30, options 15/30/50.
- Save success → persist → History refresh.
- Save failure → keep Preview + Draft.
- Discard → clear Draft → Scan → no DB write.
- parsed receipt time preferred; parse-time fallback otherwise.

## 19. Validation and Stop Rules

Validation is governed by `docs/agent/VALIDATION_LEVEL_COST_DOWN_POLICY.md` and the assigned Micro Job Issue.

Stop and return `HUMAN DECISION REQUIRED` if implementation unexpectedly requires:

- Room schema migration;
- new external dependency;
- breaking public interface change;
- major architecture change;
- substantial unrelated deletion;
- significant scope expansion;
- unrelated pre-existing failure repair;
- destructive Git operation;
- direct changes on `main`.

## 20. Design Status

**Status:** Frozen for History v1.0.  
Changes require an explicit SA decision before implementation scope is altered.
