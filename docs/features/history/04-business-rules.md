# History Business Rules

## 1. Document control

| Field | Value |
| --- | --- |
| Feature ID | `HIS` |
| Feature name | History Completion v1 |
| Document ID | `HIS-BRS-04` |
| Repository source | `04-business-rules.md` |
| Version | 0.7 |
| Status | Draft for review |
| Date | 2026-08-17 |
| Author | Victor Shih — Systems Analyst |
| Source requirements | `HIS-FRS-02`, version 0.9 |
| Source use cases | `HIS-UCS-03`, version 0.8 |
| Code baseline | `main` at `5fd0fbd` |

### 1.1 Revision history

| Version | Date | Author | Change |
| --- | --- | --- | --- |
| 0.1 | 2026-08-17 | Victor Shih | Initial business rules for History browsing, paging, deletion, receipt editing, calculations, and data integrity |
| 0.2 | 2026-08-17 | Victor Shih | Resolved FIFO ordering, Chain/Branch validation, non-negative totals, and best-effort image cleanup; retained refresh page recovery as open |
| 0.3 | 2026-08-17 | Victor Shih | Added duplicate-add rules, normalized Chain comparison, complete timestamp ordering with date-only display, and Refresh fallback to the final valid page |
| 0.4 | 2026-08-17 | Victor Shih | Preferred parsed purchase time with capture-time fallback, changed duplicate comparison to hour precision, retained current History display, and added editable Scanner Review date/time |
| 0.5 | 2026-08-17 | Victor Shih | Resolved Store identity using normalized Chain and Branch with empty-Branch equivalence |
| 0.6 | 2026-08-17 | Victor Shih | Renamed duplicate confirmation actions to Discard/Add and aligned no-insert/insert outcomes |
| 0.7 | 2026-08-17 | Victor Shih | Aligned traceability with the final duplicate dialog copy |

## 2. Purpose

This document defines reusable History Completion v1 rules that must remain
true regardless of the final Android screen layout or Java class design. It
turns the approved requirements and use-case decisions into precise validation,
calculation, paging, lifecycle, deletion, and update rules.

This document does not define control placement, colours, dialog layout, or
complete user-facing messages. Those details belong in
`05-ui-specification.md`. Concrete Given/When/Then examples belong in
`06-acceptance-tests.md`.

## 3. Source and precedence

The rules are derived from:

- `01-feature-scope.md`, version 0.8;
- `02-functional-requirements.md`, version 0.9;
- `03-use-cases.md`, version 0.8;
- approved scope decisions `OSD-HIS-01` through `OSD-HIS-20`;
- the existing receipt calculation model and persistence relationships on the
  recorded code baseline.

If this document conflicts with an approved functional requirement, the
functional requirement takes precedence and this document must be revised. An
unresolved business decision must be recorded in section 12 rather than silently
implemented.

## 4. Rule conventions

### 4.1 Rule types

| Type | Meaning |
| --- | --- |
| Constraint | Limits an allowed value or action. |
| Derivation | Calculates one value from other values. |
| Validation | Determines whether data may be saved or updated. |
| State transition | Determines the authoritative result after an event. |
| Integrity | Protects identity, relationships, or atomic data changes. |
| Lifecycle | Controls retained state, asynchronous results, or one-time effects. |

### 4.2 Rule wording

- **Must** means the rule is mandatory for History Completion v1.
- **Must not** identifies prohibited data or behaviour.
- Rule IDs remain stable. A removed rule is marked Retired and its ID is not
  reused.
- A presentation specification may choose how to display a rule outcome, but it
  must not change the rule itself.

### 4.3 Terms

| Term | Definition |
| --- | --- |
| Active mode | The History mode currently selected by the user: Receipts or All Items. |
| Retained mode state | The current page and page size remembered for one mode while the History ViewModel remains active. |
| Successful page | The most recent page whose records and paging metadata were loaded successfully for a mode. |
| Equivalent request | A request with the same mode, page number, and page size as another active request. |
| Obsolete request | A request whose mode, page, or page size is no longer the active selection when its result completes. |
| Valid item | An item satisfying all rules in `BR-HIS-EDT-05`. |
| In-memory edits | Changes entered in Edit that have not been successfully persisted. |
| Receipt aggregate | The Receipt, Store relationship, Receipt Items, Item Discounts, category references, and retained receipt metadata. |
| Owned image | A receipt image stored in app-controlled private storage. |
| Printed total | The total recognised from the receipt image; it may be unavailable. |

## 5. Rule catalogue

| Rule group | Purpose |
| --- | --- |
| `BR-HIS-BRW-*` | Defines modes, ordering, summaries, and receipt identity. |
| `BR-HIS-PAG-*` | Defines page sizes, page mathematics, navigation, and retained paging state. |
| `BR-HIS-STA-*` | Defines authoritative loading, content, empty, error, and asynchronous outcomes. |
| `BR-HIS-DEL-*` | Defines confirmation, permanent deletion, cleanup, and post-delete paging. |
| `BR-HIS-EDT-*` | Defines editable data, validation, identity preservation, and unsaved changes. |
| `BR-HIS-DUP-*` | Defines duplicate comparison, confirmation, and deliberate duplicate insertion. |
| `BR-HIS-TIM-*` | Defines purchase-timestamp source, fallback, review, precision, and persistence. |
| `BR-HIS-STR-*` | Defines normalized Store identity, reuse, and its separation from duplicate-Receipt identity. |
| `BR-HIS-CAL-*` | Defines monetary storage, subtotals, totals, and printed-total comparison. |
| `BR-HIS-DAT-*` | Defines data compatibility, execution, architecture, and lifecycle integrity. |

## 6. Detailed business rules

### 6.1 Browse, ordering, and identity

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-BRW-01` | History has exactly two in-scope modes: Receipts and All Items. Receipts is the default when no retained History state exists. | Constraint | `FR-HIS-BRW-01`, `FR-HIS-BRW-03` |
| `BR-HIS-BRW-02` | Receipt records must be ordered by the stored purchase timestamp at second precision, including hours, minutes, and seconds, from newest to oldest. Fractional seconds are normalized to zero. | Constraint | `FR-HIS-BRW-02`, `FR-HIS-BRW-10` |
| `BR-HIS-BRW-03` | All Items records must be ordered by their parent Receipt's complete purchase timestamp from newest to oldest. | Constraint | `FR-HIS-BRW-04` |
| `BR-HIS-BRW-04` | Every receipt summary must retain the Receipt ID needed to open or delete that exact receipt, even when the ID is not displayed. | Integrity | `FR-HIS-BRW-07`, `FR-HIS-DEL-04` |
| `BR-HIS-BRW-05` | An empty Branch is valid and must not prevent a receipt from being displayed, opened, updated, or deleted. | Constraint | `FR-HIS-EDT-03`, `FR-HIS-EDT-06` |
| `BR-HIS-BRW-06` | An item with no selected category is treated as Uncategorized for History and editing behaviour. | Derivation | `FR-HIS-BRW-06`, `FR-HIS-EDT-08` |
| `BR-HIS-BRW-07` | When two Receipts have the same purchase timestamp at second precision, the Receipt saved earlier must be displayed first. This FIFO order must use a stable persisted saved-order value rather than UUID lexical order. | Constraint | `FR-HIS-BRW-09` |
| `BR-HIS-BRW-08` | Receipt summaries retain the current `yyyy-MM-dd HH:mm` purchase-timestamp presentation. Omitting seconds from display must not remove or truncate the persisted seconds used for ordering. | Constraint | `FR-HIS-BRW-05`, `FR-HIS-BRW-10` |

### 6.2 Paging and retained mode state

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-PAG-01` | Allowed page sizes are 15, 30, and 50 records in both History modes. | Constraint | `FR-HIS-PAG-03` |
| `BR-HIS-PAG-02` | Without a retained selection, Receipts defaults to 15 records per page and All Items defaults to 30. | Derivation | `FR-HIS-PAG-01`, `FR-HIS-PAG-02` |
| `BR-HIS-PAG-03` | Each History mode owns an independent current page and page size while the History ViewModel remains active. Changing one mode must not overwrite the other mode's retained values. | Lifecycle | `FR-HIS-PAG-05` |
| `BR-HIS-PAG-04` | For `totalRecords > 0`, `totalPages = ceil(totalRecords / pageSize)`. For `totalRecords = 0`, `totalPages = 1` and `currentPage = 1`. | Derivation | `FR-HIS-PAG-06`, `FR-HIS-PAG-13` |
| `BR-HIS-PAG-05` | A page is valid only when `1 <= page <= totalPages`. No request may be issued for an invalid page. | Constraint | `FR-HIS-PAG-07`, `FR-HIS-PAG-08` |
| `BR-HIS-PAG-06` | The record offset for a valid page is `(page - 1) * pageSize`. | Derivation | `FR-HIS-PAG-06` |
| `BR-HIS-PAG-07` | Previous is available only when `currentPage > 1`. Next is available only when `currentPage < totalPages` and `totalRecords > 0`. | Derivation | `FR-HIS-PAG-09`, `FR-HIS-PAG-10`, `FR-HIS-PAG-13` |
| `BR-HIS-PAG-08` | Changing page size resets only the active mode to page 1 before loading with the new size. | State transition | `FR-HIS-PAG-04` |
| `BR-HIS-PAG-09` | A page selection becomes the mode's successful page only after its request succeeds; starting a request must not permanently replace the previous successful page. | State transition | `FR-HIS-PAG-11`, `FR-HIS-STA-11` |
| `BR-HIS-PAG-10` | If a page-change request fails, the previous successful records, page number, total count, total pages, and navigation availability remain authoritative. | State transition | `FR-HIS-STA-11` |
| `BR-HIS-PAG-11` | After successful deletion, if the current Receipt page becomes empty and its page number is greater than 1, the target page is `currentPage - 1`; otherwise the current valid page is reloaded. | State transition | `FR-HIS-PAG-12` |
| `BR-HIS-PAG-12` | A zero-record result is a valid Empty result on page 1 of 1, not an invalid page and not a load failure. | Derivation | `FR-HIS-PAG-13`, `FR-HIS-STA-04` |

### 6.3 Screen state and asynchronous results

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-STA-01` | The active History presentation must be derived from one consistent state snapshot rather than independently combined loading, data, error, mode, and paging values. | Integrity | `FR-HIS-STA-01` |
| `BR-HIS-STA-02` | An initial request with no successful data produces Loading until it completes; success with records produces Content, success with no records produces Empty, and failure produces Error. | State transition | `FR-HIS-STA-02` through `FR-HIS-STA-05` |
| `BR-HIS-STA-03` | Empty means a successful query returned zero records. Error means the request failed. Empty and Error must never represent the same outcome. | Constraint | `FR-HIS-STA-04`, `FR-HIS-STA-05` |
| `BR-HIS-STA-04` | Retry repeats the failed mode, page, and page-size request that produced the initial Error state. | State transition | `FR-HIS-STA-06` |
| `BR-HIS-STA-05` | Refresh operates on the active mode's current successful page and retained page size. A successful refresh replaces its records and paging metadata. | State transition | `FR-HIS-BRW-08`, `FR-HIS-STA-08` |
| `BR-HIS-STA-06` | A failed refresh with existing data keeps that data authoritative and produces one failure effect instead of replacing Content with Empty or Error. | State transition | `FR-HIS-STA-07` |
| `BR-HIS-STA-07` | At most one equivalent active load may be started for the same mode, page, and page size. | Lifecycle | `FR-HIS-STA-09` |
| `BR-HIS-STA-08` | A completed obsolete request must not replace the active mode, records, paging metadata, loading status, or error state. | Lifecycle | `FR-HIS-STA-10` |
| `BR-HIS-STA-09` | Each success, failure, confirmation, or navigation effect is consumable once and must not be emitted again only because the observer or Android lifecycle is recreated. | Lifecycle | `NFR-HIS-REL-01` |
| `BR-HIS-STA-10` | When successful Refresh produces fewer total pages than the retained current page, the target page is the refreshed final valid page; selected mode and page size remain unchanged. | State transition | `FR-HIS-STA-12` |

### 6.4 Permanent deletion

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-DEL-01` | Selecting Delete identifies a Receipt but must not alter stored data until the user explicitly confirms permanent deletion. | Constraint | `FR-HIS-DEL-01`, `FR-HIS-DEL-02` |
| `BR-HIS-DEL-02` | Cancelling the deletion confirmation performs no delete operation and leaves displayed and stored data unchanged. | State transition | `FR-HIS-DEL-03` |
| `BR-HIS-DEL-03` | Confirmed deletion must target the stable Receipt ID associated with the selected summary. | Integrity | `FR-HIS-DEL-04` |
| `BR-HIS-DEL-04` | Successful deletion removes the Receipt, all of its Receipt Items, and all Item Discounts belonging to those items. | Integrity | `FR-HIS-DEL-05` |
| `BR-HIS-DEL-05` | After database deletion succeeds, deletion of the Receipt's owned image must be attempted as best-effort cleanup. Image-deletion failure must not restore or redisplay the Receipt. | Integrity | `FR-HIS-DEL-06` |
| `BR-HIS-DEL-06` | The associated Store is deleted only when no remaining Receipt references it; a shared Store must remain. | Integrity | `FR-HIS-DEL-07` |
| `BR-HIS-DEL-07` | Deletion success depends on required database deletion. Database failure must not produce success; owned-image cleanup failure does not block success. | State transition | `FR-HIS-DEL-08`, `FR-HIS-DEL-09` |
| `BR-HIS-DEL-08` | Confirmed successful deletion is permanent and offers no Undo or recovery action. | Constraint | `FR-HIS-DEL-10` |
| `BR-HIS-DEL-09` | After deletion succeeds, Receipt paging is recalculated using `BR-HIS-PAG-04`, and the resulting page follows `BR-HIS-PAG-11`. | Derivation | `FR-HIS-PAG-12`, `FR-HIS-PAG-13` |

### 6.5 Receipt editing, validation, and identity

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-EDT-01` | Editable receipt fields are chain, optional branch, purchase date, item name, quantity, unit, unit price, and category. | Constraint | `FR-HIS-EDT-03`, `FR-HIS-EDT-04` |
| `BR-HIS-EDT-02` | Raw OCR text, printed total, receipt image, and existing Item Discounts are not directly editable. Editing must not run OCR, layout reconstruction, chain detection, or receipt parsing again. | Constraint | `FR-HIS-EDT-10` through `FR-HIS-EDT-12` |
| `BR-HIS-EDT-03` | A receipt may be initially saved or updated only when Chain is non-`null`, non-empty, and contains a non-whitespace character, and at least one valid item remains. Invalid Chain must produce field-level validation feedback; Branch is optional and may be empty. | Validation | `FR-HIS-EDT-06`, `FR-HIS-EDT-24` |
| `BR-HIS-EDT-04` | For validation, a non-empty text value contains at least one non-whitespace character. | Validation | `FR-HIS-EDT-06`, `FR-HIS-EDT-07` |
| `BR-HIS-EDT-05` | A valid item has a non-empty name, `quantity > 0`, and `unitPriceCents >= 0`. Unit is editable but is not a mandatory field in History Completion v1. | Validation | `FR-HIS-EDT-04`, `FR-HIS-EDT-07` |
| `BR-HIS-EDT-06` | An item's category must be an available child category or Uncategorized. A parent category alone is not a valid item category selection. | Validation | `FR-HIS-EDT-08` |
| `BR-HIS-EDT-07` | Changing the purchase date replaces only the calendar date and preserves the Receipt's existing time-of-day value. | Derivation | `FR-HIS-EDT-22` |
| `BR-HIS-EDT-08` | A retained item keeps its existing Item Discounts. Removing an item also removes its discounts. A newly added item begins with no discounts. | Integrity | `FR-HIS-EDT-12`, `FR-HIS-EDT-13` |
| `BR-HIS-EDT-09` | A successful update retains the original Receipt ID and must not increase the total number of Receipts. | Integrity | `FR-HIS-EDT-15` |
| `BR-HIS-EDT-10` | Updating a Receipt aggregate is atomic. Failure must leave no partial Store, Receipt, Item, Discount, or category-reference changes. | Integrity | `FR-HIS-EDT-16` |
| `BR-HIS-EDT-11` | If chain or branch changes, the updated Receipt uses the resulting Store. The previous Store is removed only when no Receipt references it. | Integrity | `FR-HIS-EDT-17` |
| `BR-HIS-EDT-12` | A successful update produces one success effect and returns to Receipt Detail for the same Receipt ID with refreshed data. | State transition | `FR-HIS-EDT-18` |
| `BR-HIS-EDT-13` | A failed update leaves the user in Edit, preserves in-memory edits, produces one failure effect, and leaves persistent data unchanged. | State transition | `FR-HIS-EDT-19` |
| `BR-HIS-EDT-14` | The explicit Cancel action abandons in-memory edits and returns to the unchanged Receipt Detail without updating persistent data. | State transition | `FR-HIS-EDT-14` |
| `BR-HIS-EDT-15` | Android Back with no changed editable value returns directly to Receipt Detail without a confirmation. | State transition | `FR-HIS-EDT-20` |
| `BR-HIS-EDT-16` | Android Back with one or more changed editable values requires an unsaved-changes confirmation before leaving Edit. | State transition | `FR-HIS-EDT-20` |
| `BR-HIS-EDT-17` | Choosing Keep Editing closes the confirmation and preserves all current in-memory edits. | State transition | `FR-HIS-EDT-21` |
| `BR-HIS-EDT-18` | Choosing `Don't Save` abandons all in-memory edits, changes no persistent data, and returns to Receipt Detail. | State transition | `FR-HIS-EDT-21` |

### 6.6 Duplicate receipt detection

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-DUP-01` | The Chain duplicate-comparison key is `lowercase(trim(chain))`. This comparison key does not require the user-facing Chain text to be stored or displayed in lowercase. | Derivation | `FR-HIS-DUP-01` |
| `BR-HIS-DUP-02` | A potential duplicate exists only when normalized Chain, purchase calendar date/hour, and final payable total in cents all equal an existing Receipt. Minute, second, fractional second, Branch, item count, item names, image, and Receipt ID are not part of this key. | Validation | `FR-HIS-DUP-02` |
| `BR-HIS-DUP-03` | Duplicate detection applies before inserting a new Receipt and after all values used by the duplicate key have passed validation and calculation. | State transition | `FR-HIS-DUP-02`, `FR-HIS-EDT-23`, `FR-HIS-EDT-24` |
| `BR-HIS-DUP-04` | No duplicate match permits normal initial save without a duplicate confirmation. | State transition | `FR-HIS-DUP-03` |
| `BR-HIS-DUP-05` | One or more duplicate matches produce one confirmation before insertion. No new Receipt may be inserted until the user selects Add. | State transition | `FR-HIS-DUP-04` |
| `BR-HIS-DUP-06` | Selecting Discard performs no insertion and retains the current reviewed draft. Selecting Add permits exactly one new Receipt insertion with its own Receipt ID. | State transition | `FR-HIS-DUP-05`, `FR-HIS-DUP-06` |
| `BR-HIS-DUP-07` | Updating a Receipt under its original Receipt ID is not an initial duplicate-add operation and must not match that Receipt against itself. | Constraint | `FR-HIS-DUP-07` |

### 6.7 Purchase timestamp

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-TIM-01` | A valid purchase date/time parsed from the receipt is the first-priority source for the draft Receipt timestamp. | Derivation | `FR-HIS-TIM-01` |
| `BR-HIS-TIM-02` | If parsing provides no valid purchase date/time, the fallback is the device-local `LocalDateTime.now()` value recorded when the image is captured or selected, not a later OCR-completion time. | Derivation | `FR-HIS-TIM-02` |
| `BR-HIS-TIM-03` | Scanner Receipt Review must show and allow modification of both purchase date and purchase time before initial save. | Constraint | `FR-HIS-TIM-03` |
| `BR-HIS-TIM-04` | The final reviewed timestamp is authoritative for duplicate comparison and persistence and is normalized to second precision by setting fractional seconds to zero. | Integrity | `FR-HIS-TIM-04` |
| `BR-HIS-TIM-05` | History editing remains distinct from initial Scanner Review: changing the date in the approved History Edit flow changes only the calendar date and preserves the stored time of day. | Constraint | `FR-HIS-EDT-22` |

### 6.8 Store identity and reuse

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-STR-01` | Store Chain identity is `lowercase(trim(chain))`; Store Branch identity is `lowercase(trim(branch))`. These normalized values are comparison keys and do not require lowercase user-facing display text. | Derivation | `FR-HIS-STR-01` |
| `BR-HIS-STR-02` | A `null`, empty, or whitespace-only Branch produces the same empty normalized Branch identity. | Derivation | `FR-HIS-STR-02` |
| `BR-HIS-STR-03` | Initial save and update must reuse one existing Store when both normalized Chain and normalized Branch match. Case or surrounding-space differences must not create another Store. | Integrity | `FR-HIS-STR-03` |
| `BR-HIS-STR-04` | Store Branch identity is independent of the duplicate-Receipt key. Duplicate detection continues to ignore Branch. | Constraint | `FR-HIS-STR-04`, `FR-HIS-DUP-02` |

### 6.9 Monetary calculations and total comparison

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-CAL-01` | Monetary values are stored and calculated as integer cents. Quantity may contain a decimal value. | Constraint | `NFR-HIS-DAT-03` |
| `BR-HIS-CAL-02` | Item original subtotal in cents is `round(quantity * unitPriceCents)`. | Derivation | Existing domain calculation; `FR-HIS-EDT-09` |
| `BR-HIS-CAL-03` | Each Item Discount amount is represented as a positive number of cents and reduces the item's payable amount. | Constraint | Existing domain calculation; `FR-HIS-EDT-12` |
| `BR-HIS-CAL-04` | Item final subtotal is `item original subtotal - sum(item discount amounts)`. | Derivation | Existing domain calculation; `FR-HIS-BRW-06` |
| `BR-HIS-CAL-05` | Receipt original total is the sum of all item original subtotals. | Derivation | Existing domain calculation |
| `BR-HIS-CAL-06` | Receipt final payable total is `sum(item final subtotals) - receipt-level total discount`. | Derivation | Existing domain calculation; `FR-HIS-BRW-05`, `FR-HIS-EDT-09` |
| `BR-HIS-CAL-07` | The calculated final payable total is recalculated after a quantity or unit-price change and after an item is added or removed. | Derivation | `FR-HIS-EDT-05`, `FR-HIS-EDT-09` |
| `BR-HIS-CAL-08` | If printed total is available and `absolute(printed total - calculated final payable total) > 1 cent`, the result is a mismatch warning and does not by itself invalidate the update. | Derivation | `FR-HIS-EDT-10` |
| `BR-HIS-CAL-09` | A difference of 0 or 1 cent is not a printed-total mismatch. An unavailable printed total cannot produce a mismatch comparison. | Derivation | `FR-HIS-EDT-10` |
| `BR-HIS-CAL-10` | Because receipt-level total discount is not an approved editable field, its saved value is retained unchanged during History editing. | Integrity | `FR-HIS-EDT-01` through `FR-HIS-EDT-04` |
| `BR-HIS-CAL-11` | An item final subtotal and the Receipt final payable total must each be zero or greater. A negative result invalidates initial save and update and must produce validation feedback. | Validation | `FR-HIS-EDT-23` |

### 6.10 Data, architecture, and lifecycle integrity

| ID | Business rule | Type | Source |
| --- | --- | --- | --- |
| `BR-HIS-DAT-01` | Existing saved receipts from before History Completion v1 must remain readable, editable, and deletable. | Integrity | `NFR-HIS-DAT-02` |
| `BR-HIS-DAT-02` | Core browse, detail, edit, update, and delete behaviour must work without an account, backend, or network connection. | Constraint | `NFR-HIS-PRV-01` |
| `BR-HIS-DAT-03` | History database and owned-image operations must execute away from the main UI thread. | Constraint | `NFR-HIS-DAT-01` |
| `BR-HIS-DAT-04` | Presentation components may call domain use cases or contracts but must not call Room DAOs or repository implementations directly. | Constraint | `NFR-HIS-ARC-01` |
| `BR-HIS-DAT-05` | Shared receipt validation must be presentation-independent and must not require History to call Scanner Fragment or Scanner ViewModel logic. | Constraint | `NFR-HIS-ARC-02` |
| `BR-HIS-DAT-06` | History page and page-size selections are retained only while the History ViewModel remains active; persistence across a full app restart is not required. | Lifecycle | Approved scope assumption |
| `BR-HIS-DAT-07` | The app must retain the existing raw OCR text, printed total, and image reference when updating a Receipt unless a separate approved feature changes them. | Integrity | `FR-HIS-EDT-10`, `FR-HIS-EDT-11` |
| `BR-HIS-DAT-08` | History labels, actions, errors, and confirmations must come from Android string resources rather than new hard-coded presentation text. | Constraint | `NFR-HIS-USE-01` |

## 7. Decision tables

### 7.1 Page calculation

| Condition | Current page | Total pages | State | Page navigation |
| --- | --- | --- | --- | --- |
| `totalRecords = 0` | 1 | 1 | Empty | Previous, Next, and other-page selection unavailable |
| `totalRecords > 0` | Requested successful page | `ceil(totalRecords / pageSize)` | Content | Derived from `BR-HIS-PAG-07` |
| Page request fails and a successful page exists | Previous successful page | Previous successful total pages | Existing Content | Restored from the successful page |
| Initial page request fails and no successful data exists | Failed request remains retryable | Not presented as a successful count | Error | Retry only |
| Refresh succeeds but retained page exceeds refreshed total pages | Refreshed final valid page | Recalculated refreshed total pages | Content or Empty | Load the refreshed final valid page |

### 7.2 Page action

| Action or condition | Result |
| --- | --- |
| Select a valid page | Request that page using the active mode's page size. |
| Select a page below 1 or above total pages | Do not issue a page request. |
| Select Previous on page 1 | Action is unavailable; no request occurs. |
| Select Previous after page 1 | Request `currentPage - 1`. |
| Select Next before the final page | Request `currentPage + 1`. |
| Select Next on the final page | Action is unavailable; no request occurs. |
| Change page size | Retain the active mode, set its page to 1, and request page 1 with the new size. |

### 7.3 Leaving Edit

| User action | Unsaved changes? | Required result |
| --- | --- | --- |
| Explicit Cancel | Either | Abandon in-memory edits and return to unchanged Receipt Detail. |
| Android Back | No | Return directly to Receipt Detail. |
| Android Back | Yes | Show unsaved-changes confirmation. |
| Keep Editing | Yes | Close confirmation and preserve in-memory edits. |
| `Don't Save` | Yes | Abandon in-memory edits and return to unchanged Receipt Detail. |

### 7.4 Update validation

| Validation condition | Valid? | Result when invalid |
| --- | --- | --- |
| Chain is non-`null`, non-empty, and contains a non-whitespace character | Required | Stay in Edit; do not call update; show red Chain input-area feedback as defined in `05-ui-specification.md`. |
| Branch | Optional | Empty Branch does not invalidate update. |
| At least one valid item remains | Required | Stay in Edit; do not call update. |
| Each item name contains a non-whitespace character | Required | Identify invalid item; do not call update. |
| Each item quantity is greater than zero | Required | Identify invalid item; do not call update. |
| Each item unit price is zero or greater | Required | Identify invalid item; do not call update. |
| Each item category is an available child or Uncategorized | Required | Identify invalid item; do not call update. |
| Every item final subtotal is zero or greater | Required | Identify the negative result; do not call save/update. |
| Receipt final payable total is zero or greater | Required | Identify the negative result; do not call save/update. |
| Printed-total mismatch is the only issue | Update remains valid | Show warning; allow update. |

### 7.5 Deletion outcome

| Condition | Success effect | Data/page outcome |
| --- | --- | --- |
| User cancels confirmation | No | No persistent or displayed change. |
| Required database deletion and image cleanup succeed | Exactly once | Receipt aggregate and owned image removed; valid Receipt page loaded. |
| Database deletion succeeds but image cleanup fails | Exactly once | Receipt remains deleted; valid Receipt page loaded; orphaned image is deferred to future maintenance cleanup. |
| Deletion empties a page greater than 1 | Exactly once after success | Previous valid Receipt page loaded. |
| Final receipt is deleted | Exactly once after success | Empty, page 1 of 1. |
| Required database deletion fails | No | Failure effect once; retain or reload a truthful valid page. |

### 7.6 Duplicate-add decision

| Duplicate-key result | User decision | Insert new Receipt? | Result |
| --- | --- | --- | --- |
| No match | Not required | Yes, once | Continue normal initial save. |
| One or more matches | No decision yet | No | Display one duplicate confirmation. |
| One or more matches | Discard | No | Close confirmation and retain the reviewed draft. |
| One or more matches | Add | Yes, once | Insert a new Receipt with its own ID and continue normal post-save behaviour. |
| Updating the same Receipt ID | Not applicable | No new Receipt | Continue the update flow without self-duplicate confirmation. |

The duplicate date/hour key is derived as:

```text
hourKey = purchaseTimestamp
        with minute = 0
        with second = 0
        with fractional second = 0
```

For example, `2026-08-17 10:05:12` and `2026-08-17 10:59:59` share the same
duplicate hour key; `2026-08-17 11:00:00` does not.

### 7.7 Store identity

| Chain input | Branch input | Normalized identity example | Store result |
| --- | --- | --- | --- |
| `Woolworths` | `Greenlane` | `woolworths` + `greenlane` | One Store identity |
| ` woolworths ` | ` greenlane ` | `woolworths` + `greenlane` | Reuse the same Store |
| `WOOLWORTHS` | `GREENLANE` | `woolworths` + `greenlane` | Reuse the same Store |
| `Woolworths` | `null`, empty, or whitespace-only | `woolworths` + empty | All three Branch forms reuse the same Store |
| `Woolworths` | `Greenlane` versus `Mt Eden` | Different normalized Branch values | Different Stores |

## 8. Calculation examples

### 8.1 Weighted or decimal-quantity item

Given quantity `1.245` and unit price `399` cents:

```text
original subtotal = round(1.245 × 399)
                  = round(496.755)
                  = 497 cents
```

### 8.2 Item discount

Given an original subtotal of `500` cents and discounts of `50` and `25` cents:

```text
final subtotal = 500 - (50 + 25)
               = 425 cents
```

### 8.3 Receipt final payable total

Given item final subtotals of `425`, `300`, and `275` cents, plus a
receipt-level discount of `100` cents:

```text
final payable total = (425 + 300 + 275) - 100
                    = 900 cents
```

### 8.4 Printed-total comparison

| Printed total | Calculated total | Absolute difference | Outcome |
| ---: | ---: | ---: | --- |
| 900 cents | 900 cents | 0 cents | No mismatch warning |
| 901 cents | 900 cents | 1 cent | No mismatch warning |
| 902 cents | 900 cents | 2 cents | Warning; update remains allowed |
| Unavailable | 900 cents | Not calculable | No mismatch comparison |

## 9. Rule-to-use-case traceability

| Rule range | Primary use cases |
| --- | --- |
| `BR-HIS-BRW-01` through `BR-HIS-BRW-08` | `UC-HIS-01`, `UC-HIS-02`, `UC-HIS-06`, `UC-HIS-07`, `UC-HIS-09`, `UC-HIS-11` |
| `BR-HIS-PAG-01` through `BR-HIS-PAG-12` | `UC-HIS-01` through `UC-HIS-05`, `UC-HIS-07`, `UC-HIS-08` |
| `BR-HIS-STA-01` through `BR-HIS-STA-10` | `UC-HIS-01` through `UC-HIS-05`, `UC-HIS-07`, `UC-HIS-08` |
| `BR-HIS-DEL-01` through `BR-HIS-DEL-09` | `UC-HIS-07` |
| `BR-HIS-EDT-01` through `BR-HIS-EDT-13` | `UC-HIS-06`, `UC-HIS-09` |
| `BR-HIS-EDT-14` through `BR-HIS-EDT-18` | `UC-HIS-10` |
| `BR-HIS-DUP-01` through `BR-HIS-DUP-07` | `UC-HIS-11` |
| `BR-HIS-TIM-01` through `BR-HIS-TIM-05` | `UC-HIS-09`, `UC-HIS-11` |
| `BR-HIS-STR-01` through `BR-HIS-STR-04` | `UC-HIS-09`, `UC-HIS-11` |
| `BR-HIS-CAL-01` through `BR-HIS-CAL-11` | `UC-HIS-01`, `UC-HIS-02`, `UC-HIS-06`, `UC-HIS-09` |
| `BR-HIS-DAT-01` through `BR-HIS-DAT-08` | `UC-HIS-01` through `UC-HIS-11` |

## 10. Rule ownership and implementation boundaries

| Rule concern | Expected owner or boundary |
| --- | --- |
| Receipt and item calculation | Domain models or reusable domain calculation service |
| Receipt and item validation | Reusable domain/application validation, shared by initial save and update |
| Page mathematics and valid-page policy | Domain/application paging result or History use case |
| Duplicate-key calculation and decision | Domain/application duplicate-check use case; presentation owns only the confirmation effect and user response |
| Timestamp source and normalization | Supermarket Parser output plus a testable clock/time provider in the Scanner application flow |
| Initial purchase date/time correction | Scanner Receipt Review presentation and immutable Scanner UI state |
| Store identity normalization and reuse | Shared domain/application normalizer plus repository/DAO candidate lookup and transactional save/update |
| Active state, retained per-mode state, and obsolete-result protection | History ViewModel and immutable UI state |
| Persistent receipt update and relationship cleanup | Repository contract and transactional data implementation |
| Best-effort owned-image deletion | Receipt image store through the repository/use-case boundary; failure does not reverse database deletion |
| Control visibility, labels, dialog wording, and layout | Presentation layer and `05-ui-specification.md` |

These ownership notes guide Clean Architecture placement. They do not prescribe
final class names or require one Java class per business rule.

## 11. Verification guidance

- Derivation rules should be verified with boundary-value unit tests.
- Validation rules should include valid, missing, zero, negative, and decimal
  cases where applicable.
- State-transition rules should verify the complete state before, during, and
  after success or failure.
- Lifecycle rules should use controlled request completion order and repeated
  observation.
- Integrity rules should inspect the complete saved aggregate and related Store
  and image outcomes.
- Every Must functional requirement must receive at least one automated or
  manual acceptance test in `06-acceptance-tests.md`.

## 12. Business-rule decisions and remaining questions

The following decisions record the Systems Analyst/Product Owner's latest
answers. A row marked Open must be resolved before its affected acceptance test
and implementation are finalised.

| ID | Decision needed | Why it matters | Recommended starting decision | Status |
| --- | --- | --- | --- | --- |
| `OBR-HIS-01` | Define a deterministic secondary order when two Receipts have the same purchase date and time. | Paging can otherwise show unstable order or repeat/skip records. | Display the Receipt saved earlier first (FIFO), using stable saved-order persistence. | Approved |
| `OBR-HIS-02` | Define whether Store Chain and Branch identity ignore leading/trailing spaces and letter case, and whether empty and `null` Branch are equivalent. | Store reuse and unused-Store cleanup require one stable identity. | Normalize both with trim/lower; treat `null`, empty, and whitespace-only Branch as equivalent. | Approved |
| `OBR-HIS-03` | Decide whether item discounts or receipt-level discounts may make a subtotal or final payable total negative. | The current formula permits negative results when discounts exceed value. | Reject initial save and update when any item final subtotal or Receipt final payable total is negative. | Approved |
| `OBR-HIS-04` | Define recovery if database deletion succeeds but owned-image deletion fails. | Database transactions and file deletion cannot form one atomic transaction. | Keep the Receipt deleted, report deletion success, and defer orphaned-image handling to a future maintenance cleanup feature. | Approved |
| `OBR-HIS-05` | Define refresh behaviour when external data changes make the retained current page greater than the new total pages. | The existing rule covers deletion initiated in History but not another data change before refresh. | Load the new final valid page: `min(previousPage, newTotalPages)`. | Approved |
| `OBR-HIS-06` | Define the purchase-timestamp source when OCR cannot recognise the printed receipt date and time. | Duplicate detection and chronological ordering need one authoritative value. | Prefer parsed receipt date/time; otherwise use capture/selection-time local `LocalDateTime.now()`; allow date/time correction in Scanner Review. | Approved |

Implementation impact for `OBR-HIS-01`: the current Receipt schema stores the
purchase date but not the save time or save sequence. Exact historical FIFO
cannot be reconstructed for existing records. Implementation therefore needs a
Room migration with a deterministic legacy backfill rule, while all Receipts
saved after migration can retain exact FIFO order.

Approved `OBR-HIS-02` means `Greenlane`, ` greenlane `, and `GREENLANE`
identify the same Branch when their normalized Chain also matches. A missing,
empty, or whitespace-only Branch likewise shares one empty Branch identity.

Example for approved `OBR-HIS-05`: with 31 Receipts and page size 15, page 3 exists and
contains one record. If that record is deleted by another data-changing flow
before Refresh, only 30 Receipts and two pages remain. Refreshing retained page
3 would then target a page that no longer exists. This is a defensive edge case
for future import, cleanup, synchronisation, or concurrent deletion paths; it is
rare in the current local-only v1 flow.

### 12.1 Current implementation gaps affecting these rules

| Area | Baseline finding at `5fd0fbd` | Required follow-up |
| --- | --- | --- |
| Room timestamp storage | `ReceiptEntity.purchaseDate` is `LocalDateTime`, and `Converters` serialises it with `ISO_LOCAL_DATE_TIME`, so hours, minutes, seconds, and currently fractional seconds can be retained. | Normalize new and edited purchase timestamps to second precision; no timestamp-type change is currently required. |
| Receipt ordering | `ReceiptDao.getReceiptsPaged` and `getAllItemsPaged` order by `purchase_date DESC` only. | Retain full-timestamp descending order and add stable FIFO secondary ordering. |
| Purchase timestamp source | `ScannerViewModel.parseDraft` currently creates `LocalDateTime.now()` after OCR; `ParsedReceipt` contains items and printed total but no purchase timestamp. | Extend Parsers/`ParsedReceipt` to return recognised date/time and capture the fallback clock value when the image is captured/selected, before OCR completes. |
| Receipt timestamp presentation | `ReceiptAdapter` already formats `yyyy-MM-dd HH:mm`. | Retain the current History presentation. Move the format/user-facing behaviour into the later UI specification and tests. |
| Scanner Review timestamp | Receipt Review currently has no purchase date/time control. | Add editable purchase-date and purchase-time controls backed by Scanner UI state. |
| Duplicate lookup | `IReceiptRepository` and `ReceiptDao` have no duplicate query. `finalPayableCents` is derived from Items and Discounts rather than stored directly on `ReceiptEntity`. | Query candidates by normalized Chain and purchase date/hour, then compare calculated final payable cents in the repository/domain; avoid adding a denormalized total unless later evidence requires it. |
| Store comparison | `ScannerViewModel` trims Chain and Branch, but `ReceiptDao.findStore` compares stored Chain and Branch exactly; lowercase normalization and empty-Branch equivalence are not consistently applied. | Centralise normalized Chain-and-Branch identity so save/update use cases, repository, and Room reuse one matching Store. |
| Optional Branch | `ScannerViewModel.createEditedReceipt` currently throws `Branch is required` when Branch is empty. | Remove this contradiction and apply the approved optional-Branch validation. |

## 13. Business-rule review checklist

The document can move from Draft to Approved when:

- every rule is consistent with `HIS-FRS-02` version 0.9 and `HIS-UCS-03`
  version 0.8;
- each calculation has an unambiguous formula and rounding point;
- required and optional edit fields are explicit;
- `Don't Save` consistently means abandoning in-memory edits without changing
  persistent data;
- paging covers first page, middle page, final page, zero records, deletion,
  and request failure;
- deletion distinguishes database, Store, and owned-image outcomes;
- decisions `OBR-HIS-01` through `OBR-HIS-06` are approved, deferred with
  an explicit impact, or removed through an approved scope change;
- developer review confirms each rule can be placed without violating MVVM or
  Clean Architecture;
- tester review confirms the rules can be converted into deterministic
  acceptance examples.

## 14. Approval

| Role | Name | Decision | Date |
| --- | --- | --- | --- |
| Product owner | Victor Shih | Pending business-rule review |  |
| Systems analyst | Victor Shih | Drafted for review | 2026-08-17 |
| Developer |  | Pending technical review |  |
| Tester |  | Pending testability review |  |
