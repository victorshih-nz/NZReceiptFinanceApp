# NZReceiptFinanceApp — History Implementation & Micro Job Plan

**Document ID:** HIP-001  
**Version:** 1.0 — Approved  
**Parent Design:** `docs/history/HISTORY_FEATURE_DESIGN.md`  
**Validation Policy:** `docs/agent/VALIDATION_LEVEL_COST_DOWN_POLICY.md`

## 1. Goal

Implement the frozen History / Receipt Items / Parsed Receipt Preview design through small, reviewable Micro Jobs.

Each Micro Job is an SA-to-Developer contract. Agent A must not redesign, split, combine, or expand a job without approval.

The existing `feature/history-functions` branch is reference/prototype material only. Reuse appropriate parts; do not blindly merge it as the implementation base.

## 2. Architecture Strategy

Reuse existing components where suitable:

- `Receipt`, `ReceiptItem`
- `SaveReceiptUseCase`
- `UpdateReceiptUseCase`
- `DeleteReceiptUseCase`
- `GetReceiptByIdUseCase`
- `GetReceiptsPagedUseCase`
- `IReceiptRepository`
- `ReceiptRepositoryImpl`
- `ReceiptDao`
- existing total/domain calculation
- existing Draft review/editing concepts

Persisted Item add/edit/delete should normally use:

```text
modify Receipt aggregate
→ UpdateReceiptUseCase
→ IReceiptRepository.updateReceipt()
→ ReceiptRepositoryImpl
→ ReceiptDao.updateFullReceipt()
```

Receipt History remains database-paged. Items for one selected Receipt are paged in presentation/ViewModel state unless later evidence requires an SA-approved alternative.

## 3. Validation Levels

Each job below has an assigned level from `VALIDATION_LEVEL_COST_DOWN_POLICY.md`.

- L2: focused unit-testable logic; local full unit/build conditional.
- L3: Android UI/integration/DI; local `assembleDebug` plus applicable focused tests.
- L4: persistence/build/cross-layer high risk; focused + local full unit + local assemble, except workflow-only special cases.

GitHub Actions is the authoritative remote gate after authorised push/PR.

---

# Batch 1 — Receipt History Baseline

## HIST-1.1 — Receipt-only History State

**Validation Level:** L3

### Goal

Align History with the target Receipt-only design.

### In Scope

- remove/deprecate global All Items mode;
- retain Receipt loading/content/empty/error state;
- keep Receipt loading through `GetReceiptsPagedUseCase`;
- adjust History Fragment/ViewModel/UI wiring as needed.

### Out of Scope

- Receipt Items editor;
- Preview;
- Receipt delete behaviour;
- Room schema.

### Acceptance

- History opens to Receipt list.
- no global All Items mode.
- Clean Architecture boundaries remain intact.

### Focused Validation

- History state/ViewModel Receipt loading tests.
- L3 local build requirements.

---

## HIST-1.2 — Receipt Paging Defaults and Controls

**Validation Level:** L3

### Goal

Implement approved Receipt paging UI/state.

### Requirements

```text
←   [2 ▼] / 6   →   [30 ▼]
```

- default 30;
- options 15/30/50;
- direct page selection;
- Previous/Next;
- page-size change returns to page 1.

### Acceptance

- paging boundaries valid;
- direct page selection works;
- controls reflect available previous/next pages.

### Focused Validation

- paging state;
- page-size changes;
- direct page selection;
- boundary cases.

---

## HIST-1.3 — Receipt Delete Confirmation and Paging Recovery

**Validation Level:** L3

### Goal

Require confirmation before Receipt delete and retain a valid page afterward.

### UI Contract

```text
[ Receipt ] 🗑
```

### Requirements

- Cancel performs no deletion;
- Confirm calls existing `DeleteReceiptUseCase` flow;
- reload/recalculate page;
- retain page if valid, otherwise new last page.

### Acceptance

- no deletion without confirmation;
- errors are surfaced;
- final-page collapse handled correctly.

### Manual Validation

Required for confirmation and paging UI.

---

# Batch 2 — Persisted Receipt Items Foundation

## HIST-2.1 — Receipt Items State and Paging

**Validation Level:** L3

### Goal

Create/adjust one-Receipt Item state and pagination.

### Required State

- loaded Receipt;
- all Items / visible page;
- page/page size/total pages;
- editing Item state;
- loading/saving/error state;
- Printed Total;
- Calculated Total.

### Requirements

- default 30;
- 15/30/50;
- direct page selection;
- Previous/Next;
- load selected Receipt with `GetReceiptByIdUseCase`.

### Acceptance

- only selected Receipt Items shown;
- no persistence mutation introduced in this job.

---

## HIST-2.2 — Read-only Item Rows and Per-item Edit Mode

**Validation Level:** L3

### Goal

Implement read-only persisted Item rows and isolated edit mode.

### UI Contract

```text
Read-only:
Milk   2   ea   $4.50   Dairy   ✎ 🗑

Editing:
[Milk] [2] [ea] [$4.80] [Dairy ▼] 💾
```

### Editable Fields

- Item Name
- Quantity
- Unit
- Unit Price
- Category
- Discount information

### Acceptance

- only selected row becomes editable;
- raw OCR name is not separately editable;
- entering edit mode alone performs no DB write;
- delete behaviour is deferred to HIST-3.2.

---

## HIST-2.3 — Persist Single Item Edit

**Validation Level:** L4

### Goal

Persist an Item only when its Save icon is tapped.

### Flow

```text
edited Item
→ validate
→ updated Receipt aggregate
→ UpdateReceiptUseCase
→ repository.updateReceipt()
→ reload/refresh
→ recalculate Calculated Total
→ preserve Printed Total
→ row read-only
→ 💾 becomes ✎
```

### Acceptance

- no update before Save;
- validation/update failure remains recoverable;
- unrelated Receipt metadata preserved;
- no new Room schema.

### Focused Validation

- success;
- validation failure;
- update failure;
- use-case invocation;
- total preservation/recalculation.

---

# Batch 3 — Persisted Item Add/Delete

## HIST-3.1 — Add Item to Existing Receipt

**Validation Level:** L4

### Goal

Add an Item to the selected Receipt using the same row UI as Edit Item.

### Flow

```text
Tap green +
→ new unsaved editable row
→ same fields as Edit Item
→ 💾
→ validate
→ persist updated Receipt aggregate
→ recalculate Calculated Total
→ preserve Printed Total
→ row read-only
→ 💾 becomes ✎
```

### Requirements

- `+` alone performs no DB write;
- no new Receipt is created;
- reuse existing project defaults only where already established.

### Acceptance

- invalid Item is not persisted;
- successful save updates aggregate and paging;
- saved row returns to normal read-only state.

---

## HIST-3.2 — Delete Item and Recover Paging

**Validation Level:** L4

### Goal

Delete one persisted Item through the Receipt aggregate update path.

### Flow

```text
Tap 🗑
→ remove Item from aggregate
→ UpdateReceiptUseCase
→ recalculate Calculated Total
→ preserve Printed Total
→ recalculate paging
→ retain current page if valid
→ otherwise new last page
```

### Acceptance

- only selected Item removed;
- atomic persisted aggregate update;
- final-page collapse handled;
- unrelated data unchanged.

### Manual Validation

Required for row delete/paging recovery.

---

# Batch 4 — Parsed Receipt Preview Modal

## SCAN-4.1 — Convert Receipt Review to Large Modal

**Validation Level:** L3

### Goal

Present parsed Draft review as a large modal rather than a normal full-screen destination.

### Requirements

- Scan remains underlying flow;
- modal uses most available screen area;
- Draft remains owned by Scanner state/ViewModel;
- opening/dismissing must not accidentally persist data.

### Manual Validation

Required on emulator/device, including many Items.

---

## SCAN-4.2 — Preview Item Paging

**Validation Level:** L3

### Requirements

- default 30;
- 15/30/50;
- direct page selection;
- Previous/Next;
- unsaved Draft edits survive page navigation;
- add/delete recalculate pages;
- final-page deletion recovers to valid page.

---

## SCAN-4.3 — Complete Preview Editable Fields

**Validation Level:** L3

### Goal

Ensure Draft Preview supports approved editable fields and Draft-only mutation.

### Fields

- Item Name
- Quantity
- Unit
- Unit Price
- Category
- Discount information

### Acceptance

- raw OCR name remains internal;
- add/edit/delete remains in memory until Save;
- Calculated Total updates;
- Printed Total preserved;
- invalid data prevents persistence.

---

## SCAN-4.4 — Preview Save → History

**Validation Level:** L4

### Flow

```text
Save
→ validate Draft
→ SaveReceiptUseCase
→ persist exactly one Receipt graph
→ close Preview
→ clear completed Draft state
→ navigate History
→ refresh Receipt History
```

History ordering follows `purchaseDate`, not save time.

### Failure Rule

- Preview stays open;
- Draft preserved;
- error shown;
- no success navigation.

### Manual Validation

Required for cross-navigation/persistence flow.

---

## SCAN-4.5 — Preview Discard → Scan

**Validation Level:** L3

### Flow

```text
Discard
→ clear Draft
→ close Preview
→ return/remain on Scan
→ no database write
```

### Acceptance

- no Receipt/Items persisted;
- stale Draft cannot reappear.

### Manual Validation

Required.

---

# Batch 5 — Receipt Date/Time Rule

## SCAN-5.1 — Parsed Date with Parse-time Fallback

**Validation Level:** L2

### Rule

```text
parsed receipt date/time available → use parsed value
otherwise → use parse-time LocalDateTime
```

### Acceptance

- assigned before Draft Preview;
- persisted Receipt preserves Draft `purchaseDate`;
- History ordering uses it.

### Focused Validation

- recognised date/time;
- missing date/time;
- invalid/unparseable date/time where supported.

If implementation unexpectedly requires Android/persistence wiring, escalate the level.

---

# Batch 6 — Regression and Human Acceptance

## HIST-6.1 — Cross-flow Regression Coverage

**Validation Level:** L4

### Required Coverage

- Save Draft → History;
- Discard Draft → Scan;
- persisted Item edit/add/delete;
- Receipt delete/Cancel;
- Printed vs Calculated totals;
- Receipt/Item paging and 15/30/50;
- final-page recovery;
- error states;
- purchase-date ordering.

---

## HIST-6.2 — Human End-to-End Android Validation

**Validation:** Human

Run the complete approved feature flow:

1. Scan/select receipt and confirm image.
2. Verify modal Preview.
3. Verify parsed time and fallback case.
4. Edit/add/delete Draft Items and paging.
5. Discard → Scan → no History record.
6. Save → History refresh.
7. Verify Receipt paging 15/30/50 and direct selection.
8. Open Receipt; verify Items read-only with ✎/🗑.
9. Edit one Item → 💾 → persisted update.
10. Verify Calculated Total changes, Printed Total does not.
11. Add persisted Item via shared editable row UI.
12. Delete persisted Item and test last-page recovery.
13. Receipt delete Cancel, then Confirm.
14. Restart app and verify persisted state.

Feature acceptance requires applicable local validation, GitHub Actions, Agent B PASS, and Human manual PASS.

## 4. Dependency Order

```text
HIST-1.1 → HIST-1.2 → HIST-1.3
HIST-1.1 → HIST-2.1 → HIST-2.2 → HIST-2.3 → HIST-3.1 → HIST-3.2
SCAN-4.1 → SCAN-4.2 → SCAN-4.3 → SCAN-4.4 → SCAN-4.5
SCAN-5.1 → SCAN-4.4
HIST-1.3 + HIST-3.2 + SCAN-4.5 → HIST-6.1 → HIST-6.2
```

One active Micro Job at a time unless Human explicitly changes the rule.

## 5. Expected Existing Components

Presentation:
- `HistoryFragment`, `HistoryViewModel`, `HistoryUiState`
- `ReceiptDetailFragment`, `ReceiptDetailViewModel`
- Receipt/Item adapters
- `ReceiptReviewFragment` / modal equivalent
- `ReceiptReviewAdapter`, `ScannerViewModel`

Domain:
- `Receipt`, `ReceiptItem`
- `GetReceiptsPagedUseCase`, `GetReceiptByIdUseCase`
- `SaveReceiptUseCase`, `UpdateReceiptUseCase`, `DeleteReceiptUseCase`
- `IReceiptRepository`

Data:
- `ReceiptRepositoryImpl`, `ReceiptDao`

DI:
- `AppContainer`, `ViewModelFactory`

These are inspection/reference points, not blanket permission to modify all of them.

## 6. Explicit Non-goals

Do not introduce manual Add Receipt, global All Items, search/filters, analytics redesign, new schema, new DI framework, Compose migration, new navigation framework, or unrelated parser/scanner refactors.

## 7. Micro Job Issue Contract

Use `.github/ISSUE_TEMPLATE/micro-job.yml`.

Each Issue must define:

- Micro Job ID;
- Parent Feature/Batch;
- Design References;
- Goal;
- Architecture Context;
- In/Out of Scope;
- Expected Components;
- Implementation Requirements;
- Acceptance Criteria;
- Validation Level;
- Focused Validation;
- Manual Validation;
- Dependencies;
- Constraints;
- Stop Conditions.

## 8. Completion Handoff

Agent A reports concise validation evidence and readiness for the next authorised Git action. It must not claim final PASS before required CI and Agent B final review.

## 9. Status

**Design:** Frozen.  
**Micro Jobs:** Defined.  
**Validation Levels:** Assigned.  
**Issue Template:** Defined.  
**Implementation authority:** One approved GitHub Micro Job Issue at a time.
