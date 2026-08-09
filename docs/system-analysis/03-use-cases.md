# 03 — Use Cases

## 1. Use-case catalogue

| ID | User goal | Primary actor | Status |
| --- | --- | --- | --- |
| UC-01 | Acquire and process a receipt image | App user | Implemented |
| UC-02 | Review and save a receipt draft | App user | Implemented |
| UC-03 | Cancel a receipt draft | App user | Implemented |
| UC-04 | Browse receipt and item history | App user | Implemented |
| UC-05 | View and delete a saved receipt | App user | Implemented |
| UC-06 | Initialise and apply category rules | System | Implemented |
| UC-07 | View category-spending analytics | App user | Partial |

## 2. UC-01 — Acquire and process a receipt image

**Goal:** Produce a classified, reviewable receipt draft from an image.

**Preconditions**

- The app is installed and Scanner is open.
- Android storage is available.
- Camera permission is granted for camera capture; it is not required for Gallery
  or Test Sample.

**Trigger:** The user taps Capture, Gallery, or Test Sample.

**Main success flow**

1. The user selects a chain or leaves Auto detect selected.
2. The user optionally enters a branch.
3. The user provides an image.
4. The system copies the image into app-private storage.
5. The system changes state to `EXTRACTING_TEXT`.
6. ML Kit recognises text lines and coordinates.
7. The system reconstructs visual receipt rows.
8. The system changes state to `PARSING_RECEIPT`.
9. Category rules are initialised if necessary.
10. The system resolves the supermarket chain.
11. The selected parser returns items and an optional printed total.
12. The system classifies each parsed item.
13. The system creates an in-memory receipt draft.
14. The system publishes `READY_FOR_REVIEW`.
15. Scanner displays the Review receipt action.

**Alternate flows**

- A1 — Camera permission denied: show a permission-denied message; no image is
  processed.
- A2 — User closes the Gallery picker: remain on Scanner in the current state.
- A3 — Explicit chain selected: skip chain auto-detection.
- A4 — Branch omitted: draft uses `Unknown Branch`, which the user can edit.
- A5 — Layout reconstruction returns no usable rows: use ML Kit flattened text.
- A6 — Category has no matching keyword: keep the item uncategorised.
- A7 — Printed total not recognised: draft total is null and Review warns the
  user to check the calculated total.

**Exception flows**

- E1 — Image cannot be copied: publish an ERROR with `Preparing image failed`.
- E2 — OCR fails: delete private image and publish an OCR ERROR.
- E3 — Chain cannot be detected: delete private image and publish a parse ERROR.
- E4 — Unsupported chain: delete private image and publish a parse ERROR.
- E5 — Parser returns no items: delete private image and publish a parse ERROR.

**Postconditions**

- Success: one private image and one in-memory draft exist; Room has no new
  receipt.
- Failure: no draft and no retained private draft image exist.

## 3. UC-02 — Review and save a receipt draft

**Goal:** Correct OCR/parser output and persist a trusted receipt.

**Preconditions**

- UC-01 completed successfully.
- `ScannerUiState` contains a draft and category list.

**Main success flow**

1. The user opens Review receipt.
2. The system displays chain, branch, raw OCR, items, categories, calculated
   total, and printed total.
3. The user reviews or edits store fields.
4. The user reviews or edits each item name, quantity, unit, price, and category.
5. The user may add or remove items.
6. The system recalculates the structured total after edits.
7. The user taps Save receipt.
8. The system validates chain, branch, and item data.
9. The state changes to `SAVING_RECEIPT`.
10. The repository maps the domain aggregate to Room entities.
11. The DAO reuses or creates the chain/branch Store.
12. Store, Receipt, Items, and Discounts are saved in one transaction.
13. The state changes to `SAVED`.
14. The system confirms save, resets Scanner state, and returns to Scanner.

**Alternate flows**

- A1 — User selects another child category: save the replacement category ID.
- A2 — Printed/calculated totals differ by more than one cent: show a warning,
  but allow save.
- A3 — Printed total is unavailable: show `Not recognised`; allow save after
  item validation.
- A4 — Store already exists: reuse the canonical Store row.

**Exception flows**

- E1 — Missing chain or branch: remain on Review and show validation message.
- E2 — No items: remain on Review and show validation message.
- E3 — Invalid item numeric input: show invalid-data warning; do not save.
- E4 — Room save fails: publish ERROR while retaining the edited draft so the
  user can retry or correct it.

**Postconditions**

- One persistent receipt aggregate exists.
- The private image remains associated with the saved receipt.
- Scanner returns to `IDLE` after the saved event is handled.

## 4. UC-03 — Cancel a receipt draft

**Goal:** Abandon a draft without leaving data or image residue.

**Precondition:** A reviewable draft exists.

**Flow**

1. The user taps the toolbar back action or Cancel.
2. The ViewModel clears Scanner state to `IDLE`.
3. The image store deletes the private draft image asynchronously.
4. Navigation returns to Scanner.

**Postcondition:** No receipt row is saved and the draft image is removed.

## 5. UC-04 — Browse receipt and item history

**Goal:** Find previously saved receipt or item information.

**Main flow**

1. The user opens History.
2. The default Receipts tab loads page zero, ten receipts per page, newest first.
3. The user may switch to All Items, which resets to page zero and loads 25
   items per page.
4. The user may move Previous/Next or pull to refresh.
5. The UI switches adapters based on the selected mode.
6. An empty message is shown when the current result list is empty.

**Exceptions and current limitations**

- Query failure displays a Toast error.
- Previous is disabled on page zero.
- Next is not currently disabled at the final page, so an empty page is possible.
- Tab selection and initial setup can trigger repeated loads; this is a known
  refactoring item.

## 6. UC-05 — View and delete a saved receipt

**Goal:** Inspect a saved receipt or remove it and its owned resources.

**View flow**

1. The user selects a receipt in History.
2. Navigation supplies `receiptId` to Receipt Detail.
3. `ReceiptDetailViewModel` loads through `GetReceiptByIdUseCase`.
4. The screen renders store, date, totals, and item details.
5. A missing receipt produces `Receipt not found`.

**Delete flow**

1. The user selects delete from a receipt history item.
2. `DeleteReceiptUseCase` calls the receipt repository.
3. The repository first loads the existing aggregate to obtain its image URI.
4. Room deletes the receipt; foreign keys cascade to items and discounts.
5. The DAO deletes the Store if no other receipt references it.
6. The image store deletes the private image.
7. History reloads the current page.

## 7. UC-06 — Initialise and apply category rules

**Primary actor:** System, triggered by UC-01.

**Flow**

1. `ParseReceiptUseCase` calls `ensureInitialized`.
2. On the first call in an app process, the initializer reads
   `category_rules.tsv`.
3. Blank, comment, and malformed rows are ignored.
4. Parent and child categories are found or created.
5. Each lowercase keyword rule is inserted/replaced.
6. `CategoryClassifier` lazily loads all rules into memory.
7. Each item name is cleaned and normalised.
8. Whole keywords are matched; the longest match wins.

**Postcondition:** Each item has either one child category or null.

## 8. UC-07 — View category-spending analytics

**Status:** Partial.

**Available domain flow**

1. Load all saved receipts.
2. Sum each item's final subtotal by category name.
3. Use `Uncategorized` for items without a category.
4. Return `CategorySpending` results.

**Missing user flow**

- No Analytics ViewModel invokes the use case.
- `AnalyticsFragment` returns an empty View.
- No chart/list, time filter, empty state, loading state, or error state exists.

UC-07 must not be marked complete until a visible, tested user outcome exists.
