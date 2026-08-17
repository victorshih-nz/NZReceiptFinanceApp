# History Use Cases

## 1. Document control

| Field | Value |
| --- | --- |
| Feature ID | `HIS` |
| Feature name | History Completion v1 |
| Document ID | `HIS-UCS-03` |
| Repository source | `03-use-cases.md` |
| Version | 0.8 |
| Status | Draft for review |
| Date | 2026-08-17 |
| Author | Victor Shih — Systems Analyst |
| Approved requirements | `HIS-FRS-02`, version 0.9 |
| Code baseline | `main` at `5fd0fbd` |

### 1.1 Revision history

| Version | Date | Author | Change |
| --- | --- | --- | --- |
| 0.1 | 2026-08-17 | Victor Shih | Initial use cases derived from approved History functional requirements version 0.2 |
| 0.2 | 2026-08-17 | Victor Shih | Renamed the discard-confirmation action to `Don't Save` and aligned with requirements version 0.3 |
| 0.3 | 2026-08-17 | Victor Shih | Added FIFO tie ordering and non-negative-total validation; aligned deletion success with best-effort image cleanup |
| 0.4 | 2026-08-17 | Victor Shih | Added duplicate-add confirmation, full purchase-timestamp ordering with date-only display, and Refresh fallback to the final valid page |
| 0.5 | 2026-08-17 | Victor Shih | Unified parsed/fallback timestamp flow, changed duplicate comparison to hour precision, retained current History display, and added editable Scanner Review date/time |
| 0.6 | 2026-08-17 | Victor Shih | Added normalized Chain-and-Branch Store identity and reuse behaviour |
| 0.7 | 2026-08-17 | Victor Shih | Renamed duplicate confirmation actions to Discard/Add and aligned their outcomes |
| 0.8 | 2026-08-17 | Victor Shih | Finalised duplicate dialog title and message as `Possible duplicate receipt` / `Add anyway?` |

## 2. Purpose

This document describes how an app user interacts with History Completion v1
to browse, navigate, refresh, inspect, delete, and correct saved receipt data.
It converts system requirements into actor-focused flows without deciding the
final Android layout or class design.

Reusable rules referenced by these flows will be formalised in
`04-business-rules.md`. Screen controls, wording, and visual states will be
defined in `05-ui-specification.md`. Test scenarios will be derived in
`06-acceptance-tests.md`.

## 3. Use-case conventions

### 3.1 Flow notation

- The **main success flow** describes the usual path that achieves the user's
  goal.
- An **alternative flow** is a valid variation that may still achieve the goal.
- An **exception flow** describes a failure or condition that prevents the
  normal outcome.
- Flow labels such as `3A` branch from the matching numbered main-flow step.
- A **success guarantee** must be true when the use case completes successfully.
- A **minimal guarantee** must remain true even when the use case fails or is
  cancelled.

### 3.2 Actors and supporting systems

**Primary actor:** App user.

**Supporting systems:**

- History presentation and ViewModel;
- History domain use cases;
- receipt repository and local Room database;
- private receipt image store;
- Scanner receipt review and its initial-save flow;
- Receipt Detail and the reusable receipt edit/review experience.

Room, the repository, and the image store support the use cases but do not make
user decisions and therefore are not primary actors.

### 3.3 Shared assumptions

- The app user is not required to sign in or use a network connection.
- Saved receipt data may already exist from an earlier app version.
- Receipt and item money values are represented as integer cents.
- History state can remain active while the user moves between History modes or
  Receipt Detail; state retention across a full app restart is not required.
- One-time success, failure, confirmation, and navigation effects are consumed
  once and are not replayed after lifecycle recreation.

## 4. Use-case catalogue

| ID | Use case | User goal | Entry point |
| --- | --- | --- | --- |
| `UC-HIS-01` | Open and browse receipt history | See saved receipts and understand the current History state | Open History |
| `UC-HIS-02` | Switch History mode | View either receipt summaries or all purchased items | Receipts / All Items selector |
| `UC-HIS-03` | Navigate History pages | Move directly or sequentially to another valid page | Page selector, Previous, or Next |
| `UC-HIS-04` | Change records per page | Choose 15, 30, or 50 records for the active mode | Page-size selector |
| `UC-HIS-05` | Refresh the current History view | Reload the current mode and page without losing context | Pull-to-refresh |
| `UC-HIS-06` | Open saved Receipt Detail | Inspect the complete saved receipt selected from History | Receipt summary |
| `UC-HIS-07` | Delete a saved receipt | Permanently remove one selected receipt after confirmation | Delete action |
| `UC-HIS-08` | Retry an initial History load | Recover from an initial content-area load failure | Retry action |
| `UC-HIS-09` | Edit and update a saved receipt | Correct a saved receipt without creating a duplicate | Edit action in Receipt Detail |
| `UC-HIS-10` | Leave receipt editing without saving | Return safely while controlling unsaved changes | Cancel or Android Back |
| `UC-HIS-11` | Decide whether to add a potential duplicate | Prevent an unintended duplicate while allowing a deliberate second copy | Confirm & save in Scanner Receipt Review |

## 5. Use-case relationships

- `UC-HIS-01` establishes the initial History mode, page, and screen state.
- `UC-HIS-02` through `UC-HIS-05` operate on the state established by
  `UC-HIS-01`.
- `UC-HIS-06` starts from a receipt displayed by `UC-HIS-01` or
  `UC-HIS-02`.
- `UC-HIS-07` starts from a receipt summary and returns to a truthful valid
  History page.
- `UC-HIS-08` extends an initial-load failure in `UC-HIS-01` or
  `UC-HIS-02`.
- `UC-HIS-09` starts from Receipt Detail opened by `UC-HIS-06`.
- `UC-HIS-10` is an alternative to completing `UC-HIS-09`.
- `UC-HIS-11` extends the initial-save flow when an existing Receipt matches the
  approved duplicate key; it does not apply to updating the same Receipt ID.

## 6. Detailed use cases

### 6.1 UC-HIS-01 — Open and browse receipt history

**Goal:** View the newest saved receipts and understand whether History is
loading, showing content, empty, or unable to load.

**Primary actor:** App user.

**Trigger:** The user opens History.

**Preconditions:**

- The app is running.
- The local database is available to the application.

**Success guarantee:** Receipt History shows the most recent successful result,
including accurate receipt summaries and paging metadata.

**Minimal guarantee:** Existing saved data is not modified.

**Main success flow:**

1. The user opens History.
2. If no retained History state exists, the system selects Receipts mode, page
   1, and the default Receipt page size of 15.
3. The system starts exactly one equivalent initial request and presents
   Loading for Receipts mode.
4. The system requests receipt records and the total record count by complete
   purchase timestamp, including hours, minutes, and seconds, from newest to
   oldest. Receipts with the same timestamp are ordered FIFO by saved order.
5. The data source returns one or more receipt records.
6. The system calculates the current page, total records, total pages, and
   Previous/Next availability.
7. The system presents Content with Chain, optional Branch, purchase timestamp
   through minutes using `yyyy-MM-dd HH:mm`, item count, and final payable total
   for each Receipt.

**Alternative and exception flows:**

- **2A — Retained state exists:** The system restores the retained mode, page,
  page size, records, and paging metadata instead of resetting them.
- **5A — No receipts exist:** The system presents Empty with zero records,
  current page 1, total pages 1, `Page 1 of 1`, and disabled page navigation.
- **5B — Initial request fails:** The system presents a History content-area
  Error with Retry and does not present the failure as Empty.
- **5C — Request becomes obsolete:** If a newer mode, page, or page-size request
  becomes active, the completed obsolete request does not replace the active
  presentation.

**Related requirements:** `FR-HIS-BRW-01`, `FR-HIS-BRW-02`,
`FR-HIS-BRW-09`, `FR-HIS-BRW-10`,
`FR-HIS-BRW-05`, `FR-HIS-PAG-01`, `FR-HIS-PAG-06`, `FR-HIS-PAG-13`,
`FR-HIS-STA-01` through `FR-HIS-STA-05`, `FR-HIS-STA-09`, and
`FR-HIS-STA-10`.

### 6.2 UC-HIS-02 — Switch History mode

**Goal:** Switch between receipt summaries and all purchased-item summaries
without losing each mode's paging choices.

**Primary actor:** App user.

**Trigger:** The user selects Receipts or All Items.

**Preconditions:**

- History is open.
- One History mode is currently selected.

**Success guarantee:** The selected mode displays its most recent successful
records and independent paging configuration.

**Minimal guarantee:** The inactive mode's retained page and page-size values
remain unchanged.

**Main success flow:**

1. The user selects the inactive History mode.
2. The system retains the current mode's page and page-size values.
3. The system selects the requested mode and restores its retained page and page
   size.
4. If All Items has no retained page size, the system uses 30; if Receipts has
   none, it uses 15.
5. The system loads the selected mode only when an equivalent active request or
   usable retained result is not already available.
6. For Receipts, the system displays receipt summaries newest-first; for All
   Items, it displays item summaries by newest parent-receipt purchase date.
7. An item summary shows item name, chain, purchase date, category or
   Uncategorized, and final subtotal.

**Alternative and exception flows:**

- **5A — Selected mode is empty:** The system presents Empty and `Page 1 of 1`
  with disabled navigation for that mode only.
- **5B — Initial load for the selected mode fails:** The system presents the
  content-area Error and Retry for that mode.
- **5C — Previous mode request completes late:** The system ignores the stale
  result and does not replace the selected mode.

**Related requirements:** `FR-HIS-BRW-03`, `FR-HIS-BRW-04`,
`FR-HIS-BRW-06`, `FR-HIS-PAG-01`, `FR-HIS-PAG-02`, `FR-HIS-PAG-05`,
`FR-HIS-PAG-13`, `FR-HIS-STA-01` through `FR-HIS-STA-05`,
`FR-HIS-STA-09`, and `FR-HIS-STA-10`.

### 6.3 UC-HIS-03 — Navigate History pages

**Goal:** Load another valid page using direct selection, Previous, or Next.

**Primary actor:** App user.

**Trigger:** The user chooses a page-navigation action.

**Preconditions:**

- History Content is visible for the selected mode.
- The requested target page exists.

**Success guarantee:** The selected valid page and its correct paging metadata
replace the previous page.

**Minimal guarantee:** A failed or invalid navigation does not replace the last
successful page with false or stale data.

**Main success flow:**

1. The user selects a valid page directly, selects Previous, or selects Next.
2. The system derives the target page using the selected mode's current page,
   total pages, and page size.
3. The system confirms that the target is between page 1 and total pages.
4. The system requests the target page without changing the inactive mode's
   state.
5. The request succeeds with records and paging metadata.
6. The system replaces the active records and marks the target as the most
   recent successful page.
7. Previous is disabled on page 1; Next is disabled on the final page.

**Alternative and exception flows:**

- **1A — Direct page selection:** The user may select any first, middle, or
  final valid page.
- **3A — Invalid target:** The system does not issue a request for a page below
  1 or above total pages.
- **5A — Page-change request fails:** The system keeps the previous successful
  records, restores its page number and metadata, and produces one failure
  effect.
- **5B — Request becomes obsolete:** A result for an inactive mode, old page,
  or old page size is ignored.
- **7A — Zero records:** The only logical page is page 1 of 1; Previous, Next,
  and other page choices are unavailable.

**Related requirements:** `FR-HIS-PAG-06` through `FR-HIS-PAG-11`,
`FR-HIS-PAG-13`, `FR-HIS-STA-10`, and `FR-HIS-STA-11`.

### 6.4 UC-HIS-04 — Change records per page

**Goal:** Change the number of records displayed per page for the active mode.

**Primary actor:** App user.

**Trigger:** The user chooses 15, 30, or 50 from the page-size selector.

**Preconditions:** History is open in Receipts or All Items mode.

**Success guarantee:** The active mode is reloaded at page 1 using the selected
page size, and the inactive mode's paging choices are unchanged.

**Minimal guarantee:** An unsupported page size is not applied.

**Main success flow:**

1. The user opens the page-size selector.
2. The system offers 15, 30, and 50.
3. The user selects one offered value.
4. The system stores the selection for the active mode while History state
   remains active.
5. The system resets only the active mode to page 1.
6. The system requests page 1 using the selected page size.
7. The system displays the returned records and recalculated total pages.

**Alternative and exception flows:**

- **3A — User retains the current value:** No equivalent reload is required.
- **6A — Empty result:** The system shows Empty and `Page 1 of 1` with the new
  page size retained.
- **6B — Older request completes later:** The system ignores a result associated
  with the previous page size.

**Related requirements:** `FR-HIS-PAG-01` through `FR-HIS-PAG-06`,
`FR-HIS-PAG-08`, `FR-HIS-PAG-11`, `FR-HIS-PAG-13`, and
`FR-HIS-STA-10`.

### 6.5 UC-HIS-05 — Refresh the current History view

**Goal:** Reload the currently displayed mode and page without losing paging
context.

**Primary actor:** App user.

**Trigger:** The user performs pull-to-refresh.

**Preconditions:** History is open and a mode is selected.

**Success guarantee:** The active mode displays the newest records and paging
metadata for the same page and page size, or for the new final valid page when
the retained page no longer exists.

**Minimal guarantee:** If refresh fails, the last successful records remain
visible and no persistent data is changed.

**Main success flow:**

1. The user performs pull-to-refresh.
2. The system retains the selected mode, current page, and page size.
3. The system requests that same mode, page, and page size.
4. The request succeeds.
5. The system replaces the displayed records, total records, total pages, and
   paging availability with the refreshed result.

**Alternative and exception flows:**

- **4A — Refresh fails with existing data:** The system keeps the existing data
  visible and produces one refresh-failure effect.
- **4B — Refresh result becomes obsolete:** A newer mode, page, or page-size
  request remains authoritative.
- **5A — Refresh produces zero records:** The system displays Empty and
  `Page 1 of 1` for the selected mode.
- **5B — Retained page no longer exists:** The system loads the new final valid
  page, retains the selected mode and page size, and displays that page's
  records and paging metadata.

**Related requirements:** `FR-HIS-BRW-08`, `FR-HIS-PAG-13`,
`FR-HIS-STA-07`, `FR-HIS-STA-08`, `FR-HIS-STA-10`, and
`FR-HIS-STA-12`.

### 6.6 UC-HIS-06 — Open saved Receipt Detail

**Goal:** Inspect the complete saved receipt represented by a History summary.

**Primary actor:** App user.

**Trigger:** The user selects a receipt summary.

**Preconditions:** Receipts mode displays at least one receipt summary.

**Success guarantee:** Receipt Detail shows the saved receipt matching the
selected Receipt ID.

**Minimal guarantee:** Selecting a summary does not modify the receipt.

**Main success flow:**

1. The user selects a receipt summary.
2. The system reads the Receipt ID associated with that summary.
3. The system navigates to Receipt Detail using that Receipt ID.
4. Receipt Detail loads and displays the matching saved receipt.
5. Receipt Detail provides an Edit action for that receipt.

**Alternative and exception flows:**

- **4A — Receipt can no longer be loaded:** Receipt Detail reports an accurate
  load failure and does not display another receipt as the selection.

**Related requirements:** `FR-HIS-BRW-07` and `FR-HIS-EDT-01`.

### 6.7 UC-HIS-07 — Delete a saved receipt

**Goal:** Permanently remove one selected receipt after an explicit decision.

**Primary actor:** App user.

**Trigger:** The user selects Delete for a receipt summary.

**Preconditions:**

- Receipts mode displays the target receipt.
- The target receipt has a stable Receipt ID.

**Success guarantee:** The selected receipt aggregate is removed from the
database, any now-unused Store is removed, private-image deletion is attempted,
and History displays a truthful valid page.

**Minimal guarantee:** Before confirmation, nothing is deleted. If deletion
fails, success is not reported and the visible state remains truthful.

**Main success flow:**

1. The user selects Delete for one receipt.
2. The system presents a permanent-deletion confirmation for that receipt and
   does not call the delete operation yet.
3. The user confirms deletion.
4. The system sends the selected Receipt ID through the Delete Receipt domain
   use case.
5. The system removes the Receipt, its Receipt Items, and their Item Discounts
   as one consistent delete operation.
6. The system attempts to remove the app-owned private receipt image.
7. The system removes the associated Store only if no other Receipt references
   it.
8. After required database deletion succeeds, the system produces one
   deletion-success effect; private-image cleanup does not block success.
9. The system reloads the current Receipt page.
10. History presents the resulting valid page without an Undo action.

**Alternative and exception flows:**

- **3A — User cancels confirmation:** The system closes the confirmation; no
  delete operation runs and displayed and stored data remain unchanged.
- **5A — Database deletion fails:** The system does not report success,
  produces one failure effect, and retains or reloads a truthful valid page.
- **6A — Private-image deletion fails:** The Receipt remains deleted, deletion
  success is still reported, and the orphaned image is left for a future data
  maintenance/cleanup feature.
- **9A — Deletion emptied a non-first page:** The system loads the previous
  valid Receipt page.
- **9B — Deletion removed the final receipt:** The system shows Empty with zero
  records and `Page 1 of 1`.
- **10A — Shared Store remains in use:** The Store is retained for other
  receipts.

**Related requirements:** `FR-HIS-PAG-12`, `FR-HIS-PAG-13`, and
`FR-HIS-DEL-01` through `FR-HIS-DEL-10`.

### 6.8 UC-HIS-08 — Retry an initial History load

**Goal:** Retry the same request after History could not initially load a mode.

**Primary actor:** App user.

**Trigger:** The user selects Retry in the History content-area Error state.

**Preconditions:**

- The selected mode has no existing successfully loaded data.
- Its most recent initial request failed.

**Success guarantee:** The repeated request results in Content or Empty for the
same mode, page, and page size.

**Minimal guarantee:** A repeated failure remains an Error and is not presented
as an empty successful result.

**Main success flow:**

1. The user selects Retry.
2. The system repeats the failed mode, page, and page-size request.
3. The system presents Loading.
4. The request succeeds.
5. The system presents Content when records exist or Empty with `Page 1 of 1`
   when none exist.

**Alternative and exception flows:**

- **4A — Retry fails:** The system returns to the content-area Error state and
  continues to offer Retry.
- **4B — Retry becomes obsolete:** A newer active request remains authoritative.

**Related requirements:** `FR-HIS-PAG-13`, `FR-HIS-STA-02` through
`FR-HIS-STA-06`, and `FR-HIS-STA-10`.

### 6.9 UC-HIS-09 — Edit and update a saved receipt

**Goal:** Correct an existing saved receipt while retaining its Receipt ID.

**Primary actor:** App user.

**Trigger:** The user selects Edit in Receipt Detail.

**Preconditions:**

- Receipt Detail has successfully loaded a saved receipt.
- The receipt has a stable Receipt ID.

**Success guarantee:** The complete valid aggregate is atomically updated under
the original Receipt ID, and the refreshed Receipt Detail is displayed.

**Minimal guarantee:** A failed update creates no partial data or duplicate
receipt and preserves the user's editable values for correction or retry.

**Main success flow:**

1. The user selects Edit.
2. The system loads the saved Receipt aggregate into the receipt edit
   experience.
3. The system presents editable chain, optional branch, purchase date, and item
   fields; raw OCR text, printed total, receipt image, and Item Discounts remain
   read-only or unavailable for direct editing.
4. The user changes any permitted receipt or item fields and may add or remove
   items.
5. If the purchase date changes, the system applies the new calendar date and
   retains the existing time of day.
6. The system recalculates and displays the final payable total as editable
   item values change.
7. If the printed total differs from the calculated total by more than one
   cent, the system displays a warning without automatically blocking update.
8. The user selects the update/save action.
9. The system validates a non-empty chain and at least one valid item; Branch
   may be empty. A `null`, empty, or whitespace-only Chain is invalid.
10. The system validates each item has a non-empty name, quantity greater than
    zero, unit price greater than or equal to zero, and an available child
    category or Uncategorized.
11. The system validates that no item final subtotal and no Receipt final
    payable total is negative.
12. The system atomically updates the Store, Receipt, Receipt Items, retained
    Item Discounts, and category references under the original Receipt ID.
13. A removed item and its discounts are deleted; a newly added item begins
    with no discounts.
14. If Chain or Branch changed, the system normalizes both identity values with
    `trim` and lowercase comparison, reuses a matching Store, and removes the
    previous Store only when no Receipt still references it. A `null`, empty,
    or whitespace-only Branch has the same empty identity.
15. The system produces one update-success effect.
16. The system returns to Receipt Detail and displays the updated receipt.

**Alternative and exception flows:**

- **4A — Branch is empty:** The edit remains valid if chain and all item rules
  pass.
- **9A — Receipt validation fails:** The system remains in Edit, identifies the
  invalid input, and does not call the update operation. If Chain is invalid,
  its input area uses the red validation presentation defined in the UI
  specification.
- **10A — Item validation fails:** The system remains in Edit, identifies the
  invalid item, and does not call the update operation.
- **11A — A calculated amount is negative:** The system remains in Edit,
  displays validation feedback, and does not call the update operation.
- **12A — Update fails at any point:** The transaction rolls back, no partial
  Store, Receipt, Item, Discount, or category-reference change remains, the
  user's editable values remain visible, and one failure effect is produced.
- **14A — Previous Store is shared:** The system retains that Store.
- **16A — Lifecycle is recreated after success:** The consumed success and
  navigation effects are not replayed.

**Related requirements:** `FR-HIS-EDT-02` through `FR-HIS-EDT-13`,
`FR-HIS-EDT-15` through `FR-HIS-EDT-19`, `FR-HIS-EDT-22`, and
`FR-HIS-EDT-23` through `FR-HIS-EDT-24`, and `FR-HIS-STR-01` through
`FR-HIS-STR-03`.

### 6.10 UC-HIS-10 — Leave receipt editing without saving

**Goal:** Leave Edit without unintentionally persisting or discarding changes.

**Primary actor:** App user.

**Trigger:** The user selects the explicit Cancel action or requests Android
Back.

**Preconditions:** The saved receipt edit experience is open.

**Success guarantee:** The user returns to Receipt Detail without changing
persistent receipt data.

**Minimal guarantee:** Unsaved changes are not persisted without the update/save
action.

**Main success flow — explicit Cancel:**

1. The user selects Cancel.
2. The system discards the in-memory edits.
3. The system returns to Receipt Detail.
4. Receipt Detail continues to show the unchanged saved receipt.

**Alternative flow — Android Back:**

1. The user requests Android Back.
2. If no editable value has changed, the system returns directly to Receipt
   Detail.
3. If an editable value has changed, the system presents a discard-changes
   confirmation before leaving.
4. If the user chooses Keep Editing, the confirmation closes and all current
   editable values remain.
5. If the user chooses `Don't Save`, the system discards the in-memory edits and
   returns to Receipt Detail without changing persistent data.

**Related requirements:** `FR-HIS-EDT-14`, `FR-HIS-EDT-20`, and
`FR-HIS-EDT-21`.

### 6.11 UC-HIS-11 — Review and decide whether to add a potential duplicate

**Goal:** Avoid accidentally inserting a previously saved receipt while still
allowing an intentional second copy.

**Primary actor:** App user.

**Trigger:** The user selects Confirm & save for a new Receipt draft.

**Preconditions:**

- Scanner Receipt Review contains a valid new Receipt draft.
- Chain and final payable total have passed validation.
- A parsed receipt purchase timestamp or capture-time fallback is available on
  the draft.

**Success guarantee:** A unique Receipt is saved normally, or a matching Receipt
is added exactly once only after the user selects Add.

**Minimal guarantee:** A matching Receipt is not inserted before the user makes
an explicit decision; selecting Discard preserves the draft and makes no
database change.

**Main success flow:**

1. When the Parser recognises a valid receipt purchase date and time, the
   system places it in the draft; otherwise the system uses the local
   `LocalDateTime.now()` value recorded for image capture/selection.
2. Scanner Receipt Review displays the draft purchase date and time.
3. The user may modify the purchase date, purchase time, or both.
4. The user selects Confirm & save.
5. The system normalizes the final reviewed timestamp to second precision and
   normalizes Chain using `trim` and lowercase conversion.
6. Away from the main UI thread, the system checks for an existing Receipt with
   the same normalized Chain, purchase calendar date/hour, and final payable
   total in cents. Minutes and seconds are ignored for this comparison.
7. A matching Receipt exists.
8. The system displays title `Possible duplicate receipt`, message
   `Add anyway?`, and Discard/Add actions and does not insert the draft yet.
9. The user selects Add.
10. The system reuses a Store whose normalized Chain and Branch identity match,
    or creates one Store when none matches, then inserts the new Receipt exactly
    once.
11. The system reports save success and completes the normal post-save
   navigation.

**Alternative and exception flows:**

- **1A — Parser does not recognise date/time:** The capture/selection-time local
  timestamp remains the editable fallback in Receipt Review.
- **7A — No matching Receipt exists:** The system skips the duplicate
  confirmation and performs the normal initial save exactly once.
- **9A — User selects Discard:** The system closes the confirmation, inserts
  nothing, and retains the reviewed draft for correction or cancellation.
- **10A — Insertion fails after Add:** The system reports save failure once,
  inserts no partial aggregate, and retains the reviewed draft.
- **Update exclusion:** Updating an existing Receipt under its original ID does
  not compare that Receipt with itself or display the duplicate-add prompt.

**Related requirements:** `FR-HIS-DUP-01` through `FR-HIS-DUP-07`,
`FR-HIS-EDT-06`, `FR-HIS-EDT-23`, `FR-HIS-EDT-24`, and
`FR-HIS-TIM-01` through `FR-HIS-TIM-04`, `FR-HIS-STR-01` through
`FR-HIS-STR-04`, and `NFR-HIS-DAT-01`.

## 7. Cross-cutting use-case guarantees

The following constraints apply across the detailed use cases:

- History presentation calls domain use cases or contracts rather than Room
  DAOs or repository implementations directly.
- Receipt validation is reusable domain/application logic and does not require
  History to call Scanner Fragment or Scanner ViewModel code.
- History read, count, duplicate lookup, save, update, delete, and image work
  executes away from the main UI thread.
- Private-image cleanup is best-effort after successful database deletion and
  does not restore a deleted Receipt.
- Initial save and update reject negative calculated subtotals or final payable
  totals.
- Initial save prefers parsed receipt purchase date/time, falls back to the
  capture/selection-time local value, and permits date/time correction in
  Scanner Receipt Review.
- Browse, edit, and delete remain available offline and without an account.
- Existing saved receipts remain readable, editable, and deletable.
- User-facing History text comes from Android string resources.
- Each one-time effect is consumed once.
- Each mandatory functional requirement will trace to an automated or manual
  acceptance test in `06-acceptance-tests.md`.

## 8. Requirements traceability

| Use case | Functional requirements covered |
| --- | --- |
| `UC-HIS-01` | `FR-HIS-BRW-01`, `FR-HIS-BRW-02`, `FR-HIS-BRW-05`, `FR-HIS-BRW-09`, `FR-HIS-BRW-10`, `FR-HIS-PAG-01`, `FR-HIS-PAG-06`, `FR-HIS-PAG-13`, `FR-HIS-STA-01` through `FR-HIS-STA-05`, `FR-HIS-STA-09`, `FR-HIS-STA-10` |
| `UC-HIS-02` | `FR-HIS-BRW-03`, `FR-HIS-BRW-04`, `FR-HIS-BRW-06`, `FR-HIS-PAG-01`, `FR-HIS-PAG-02`, `FR-HIS-PAG-05`, `FR-HIS-PAG-13`, `FR-HIS-STA-01` through `FR-HIS-STA-05`, `FR-HIS-STA-09`, `FR-HIS-STA-10` |
| `UC-HIS-03` | `FR-HIS-PAG-06` through `FR-HIS-PAG-11`, `FR-HIS-PAG-13`, `FR-HIS-STA-10`, `FR-HIS-STA-11` |
| `UC-HIS-04` | `FR-HIS-PAG-01` through `FR-HIS-PAG-06`, `FR-HIS-PAG-08`, `FR-HIS-PAG-11`, `FR-HIS-PAG-13`, `FR-HIS-STA-10` |
| `UC-HIS-05` | `FR-HIS-BRW-08`, `FR-HIS-PAG-13`, `FR-HIS-STA-07`, `FR-HIS-STA-08`, `FR-HIS-STA-10`, `FR-HIS-STA-12` |
| `UC-HIS-06` | `FR-HIS-BRW-07`, `FR-HIS-EDT-01` |
| `UC-HIS-07` | `FR-HIS-PAG-12`, `FR-HIS-PAG-13`, `FR-HIS-DEL-01` through `FR-HIS-DEL-10` |
| `UC-HIS-08` | `FR-HIS-PAG-13`, `FR-HIS-STA-02` through `FR-HIS-STA-06`, `FR-HIS-STA-10` |
| `UC-HIS-09` | `FR-HIS-EDT-02` through `FR-HIS-EDT-13`, `FR-HIS-EDT-15` through `FR-HIS-EDT-19`, `FR-HIS-EDT-22` through `FR-HIS-EDT-24`, `FR-HIS-STR-01` through `FR-HIS-STR-03` |
| `UC-HIS-10` | `FR-HIS-EDT-14`, `FR-HIS-EDT-20`, `FR-HIS-EDT-21` |
| `UC-HIS-11` | `FR-HIS-DUP-01` through `FR-HIS-DUP-07`, `FR-HIS-EDT-06`, `FR-HIS-EDT-23`, `FR-HIS-EDT-24`, `FR-HIS-TIM-01` through `FR-HIS-TIM-04`, `FR-HIS-STR-01` through `FR-HIS-STR-04` |

| Non-functional requirement | Applicable use cases |
| --- | --- |
| `NFR-HIS-ARC-01` | `UC-HIS-01` through `UC-HIS-11` |
| `NFR-HIS-ARC-02` | `UC-HIS-09`, `UC-HIS-10` |
| `NFR-HIS-DAT-01` | `UC-HIS-01` through `UC-HIS-09`, `UC-HIS-11` |
| `NFR-HIS-DAT-02` | `UC-HIS-01`, `UC-HIS-06`, `UC-HIS-07`, `UC-HIS-09` |
| `NFR-HIS-DAT-03` | `UC-HIS-01`, `UC-HIS-02`, `UC-HIS-06`, `UC-HIS-09` |
| `NFR-HIS-PRV-01` | `UC-HIS-01` through `UC-HIS-11` |
| `NFR-HIS-CMP-01` | `UC-HIS-01` through `UC-HIS-11` |
| `NFR-HIS-USE-01` | `UC-HIS-01` through `UC-HIS-11` |
| `NFR-HIS-REL-01` | `UC-HIS-01` through `UC-HIS-11` |
| `NFR-HIS-TST-01` | `UC-HIS-01` through `UC-HIS-11` |

## 9. Use-case review checklist

The document can move from Draft to Approved when:

- each user goal is represented by one clear use case;
- every use case identifies trigger, preconditions, guarantees, main flow, and
  relevant alternative or exception flows;
- every functional requirement in `HIS-FRS-02` version 0.9 is covered by at
  least one use case;
- no flow introduces search, filters, Undo, OCR reprocessing, image editing, or
  another excluded capability;
- product-owner review confirms that the interaction sequences match intended
  user behaviour;
- developer review confirms technical feasibility without changing user intent;
- tester review confirms that the flows can be converted into acceptance tests.

## 10. Approval

| Role | Name | Decision | Date |
| --- | --- | --- | --- |
| Product owner | Victor Shih | Pending use-case review |  |
| Systems analyst | Victor Shih | Drafted for review | 2026-08-17 |
| Developer |  | Pending technical review |  |
| Tester |  | Pending testability review |  |
