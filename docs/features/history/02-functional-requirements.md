# History Functional Requirements

## 1. Document control

| Field | Value |
| --- | --- |
| Feature ID | `HIS` |
| Feature name | History Completion v1 |
| Document ID | `HIS-FRS-02` |
| Repository source | `02-functional-requirements.md` |
| Version | 1.0 |
| Status | Approved for use-case analysis |
| Date | 2026-08-17 |
| Author | Victor Shih — Systems Analyst |
| Approved scope | `HIS-SCP-01`, version 0.9 |
| Code baseline | `main` at `5fd0fbd` |

### 1.1 Revision history

| Version | Date | Author | Change |
| --- | --- | --- | --- |
| 0.1 | 2026-08-17 | Victor Shih | Initial functional and non-functional requirements derived from the approved History scope |
| 0.2 | 2026-08-17 | Victor Shih | Resolved OQR-HIS-01 through OQR-HIS-05; made Branch optional; defined date-only editing, unsaved-change handling, page-change failure recovery, and zero-record paging |
| 0.3 | 2026-08-17 | Victor Shih | Renamed the discard-confirmation action to `Don't Save` |
| 0.4 | 2026-08-17 | Victor Shih | Added FIFO tie ordering and non-negative-total validation; made private-image cleanup best-effort after database deletion |
| 0.5 | 2026-08-17 | Victor Shih | Added duplicate-add confirmation, normalized Chain comparison, full purchase-timestamp ordering, and Refresh fallback to the final valid page |
| 0.6 | 2026-08-17 | Victor Shih | Preferred parsed purchase time with capture-time fallback, changed duplicate comparison to hour precision, retained current History timestamp display, and made Scanner Review date/time editable |
| 0.7 | 2026-08-17 | Victor Shih | Defined normalized Chain-and-Branch Store identity, including empty and `null` Branch equivalence |
| 0.8 | 2026-08-17 | Victor Shih | Renamed duplicate confirmation actions to Discard/Add and defined the Discard outcome |
| 0.9 | 2026-08-17 | Victor Shih | Finalised duplicate dialog title and message as `Possible duplicate receipt` / `Add anyway?` |
| 1.0 | 2026-08-17 | Victor Shih | Revised Store and duplicate Chain/Branch normalization to retain ASCII letters and digits only |

## 2. Purpose

This document translates the approved History Completion v1 scope into uniquely
identified and testable system requirements. It defines required system
behaviour without prescribing Fragment, ViewModel, Room, or SQL implementation
details unless an approved architecture constraint requires them.

Detailed interaction sequences will be defined in `03-use-cases.md`; reusable
rules in `04-business-rules.md`; screen layout and messages in
`05-ui-specification.md`; and executable acceptance examples in
`06-acceptance-tests.md`.

## 3. Source and precedence

Requirements in this document are derived from:

- `01-feature-scope.md`, version 0.8;
- approved decisions `OSD-HIS-01` through `OSD-HIS-20`;
- current History, Receipt Detail, Receipt Review, repository, and Room
  behaviour on the recorded code baseline;
- project-wide MVVM and Clean Architecture constraints.

If these sources conflict, the approved feature scope and the resolved
decisions in section 9 take precedence. Any future ambiguity shall be recorded
rather than silently resolved.

## 4. Requirement conventions

### 4.1 Requirement wording

- **Shall** identifies mandatory, testable behaviour.
- **Should** identifies expected behaviour that may be deferred only through an
  approved scope or priority change.
- Each requirement ID remains stable. A removed requirement is marked Retired;
  IDs are not reused.

### 4.2 Priority

| Priority | Meaning |
| --- | --- |
| Must | Required for History Completion v1 acceptance |
| Should | Important but may be deferred through an approved change |
| Could | Optional enhancement outside the minimum completion outcome |

### 4.3 Terms

| Term | Definition |
| --- | --- |
| History mode | Either Receipts or All Items. |
| Initial load | First data request when no successfully loaded records exist for the requested mode. |
| Refresh | User-requested reload of the currently displayed mode and page. |
| Valid page | A page number from 1 through `totalPages`; when no records exist, current page and total pages are both 1 and the screen remains in Empty state. |
| Existing data | The most recent successfully loaded records still held in History state. |
| One-time effect | A message or navigation action consumed once and not replayed as persistent screen state. |
| Receipt aggregate | Store, Receipt, Receipt Items, Item Discounts, category references, and retained receipt metadata. |
| Same receipt | A receipt retaining its original Receipt ID after update. |

## 5. Functional requirements

### 5.1 Browse modes and summaries

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-BRW-01` | When History is opened without retained History state, the system shall select Receipts mode. | Must | Open History from a new app/session state; Receipts is selected. |
| `FR-HIS-BRW-02` | The system shall load saved Receipts in complete-purchase-timestamp order, including hours, minutes, and seconds, from newest to oldest. | Must | Compare query/result order using stored timestamps that differ at hour, minute, and second boundaries. |
| `FR-HIS-BRW-03` | The system shall allow the user to switch between Receipts and All Items modes. | Must | Select each tab and verify the corresponding record type. |
| `FR-HIS-BRW-04` | The system shall load and display purchased items in newest-receipt-purchase-date-first order. | Must | Compare item order with parent receipt dates. |
| `FR-HIS-BRW-05` | A Receipt summary shall display Chain, optional Branch, purchase timestamp through minutes using the current `yyyy-MM-dd HH:mm` presentation, item count, and final payable total. | Must | Verify every approved field and timestamp presentation against the selected Receipt aggregate. |
| `FR-HIS-BRW-06` | An item summary shall display item name, chain, purchase date, category or Uncategorized, and final subtotal. | Must | Verify every approved field against the item and parent Receipt. |
| `FR-HIS-BRW-07` | Selecting a receipt summary shall open Receipt Detail for that receipt's ID. | Must | Select a row and verify the matching Receipt Detail. |
| `FR-HIS-BRW-08` | Pull-to-refresh shall reload the currently selected mode, page number, and page size without switching modes. | Must | Refresh each mode from a non-default page configuration. |
| `FR-HIS-BRW-09` | When two Receipts have the same purchase date and time, the Receipt saved earlier shall be displayed before the Receipt saved later. | Must | Save two Receipts with the same purchase timestamp and verify stable FIFO order across repeated loads and pages. |
| `FR-HIS-BRW-10` | The system shall persist a Receipt purchase timestamp at second precision, retaining hours, minutes, and seconds and normalizing fractional seconds to zero; History may display the same value only through minutes. | Must | Save and reload a Receipt, verify second precision with no fractional component, and verify the current minute-level History display. |

### 5.2 Pagination and page-size selection

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-PAG-01` | Receipts mode shall use 15 records per page when it has no retained page-size selection. | Must | Open new History state and inspect the first Receipt query/result. |
| `FR-HIS-PAG-02` | All Items mode shall use 30 records per page when it has no retained page-size selection. | Must | Open All Items in new History state and inspect the first query/result. |
| `FR-HIS-PAG-03` | Both modes shall allow page-size selection from 15, 30, and 50 records. | Must | Select every value in both modes and verify record limits. |
| `FR-HIS-PAG-04` | Changing page size shall reset only the affected mode to page 1 and reload it using the selected size. | Must | Change size from a later page and verify page 1 and the new limit. |
| `FR-HIS-PAG-05` | Receipts and All Items shall retain independent current-page and page-size values while History state remains active. | Must | Configure both modes differently, switch repeatedly, and verify restoration. |
| `FR-HIS-PAG-06` | For a successful non-empty query, the system shall determine and expose total records, total pages, current page, and page size. | Must | Compare displayed metadata with known test data counts. |
| `FR-HIS-PAG-07` | The system shall allow the user to select and load any valid page directly. | Must | Select first, middle, and final valid pages. |
| `FR-HIS-PAG-08` | The system shall prevent requests for page numbers below 1 or above `totalPages`. | Must | Attempt invalid selections and verify no invalid query/navigation occurs. |
| `FR-HIS-PAG-09` | Previous shall be disabled on page 1 and shall load `currentPage - 1` on other pages. | Must | Verify button state and result on first and later pages. |
| `FR-HIS-PAG-10` | Next shall be disabled on the final page and shall load `currentPage + 1` when another page exists. | Must | Verify button state and result before and at the final page. |
| `FR-HIS-PAG-11` | Paging controls shall represent only the most recent successfully loaded mode and page request. | Must | Complete requests in a different order and verify stale results are ignored. |
| `FR-HIS-PAG-12` | If successful deletion empties a non-first Receipt page, the system shall load the previous valid Receipt page. | Must | Delete the only record on a later page and verify repositioning. |
| `FR-HIS-PAG-13` | When a mode has zero records, the system shall expose current page 1 and total pages 1, display `Page 1 of 1`, and disable Previous, Next, and selection of any other page. | Must | Load an empty result and verify Empty state, page text, and disabled navigation. |

### 5.3 Loading, content, empty, and error behaviour

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-STA-01` | History shall expose one consistent screen state containing selected mode, each mode's paging values, displayed records, loading status, paging availability, and screen error. | Must | State tests verify that one snapshot contains all required values. |
| `FR-HIS-STA-02` | An initial request shall place the requested mode in a Loading state until it succeeds or fails. | Must | Delay the data source and verify Loading before completion. |
| `FR-HIS-STA-03` | A successful request containing records shall display Content and remove any initial-load error presentation. | Must | Return records after loading/error and verify Content. |
| `FR-HIS-STA-04` | A successful initial request containing no records shall display an Empty state and shall not display an error. | Must | Return an empty successful result. |
| `FR-HIS-STA-05` | An initial request failure with no existing data shall display an Error state within the History content area. | Must | Fail the first request and verify Error instead of Empty. |
| `FR-HIS-STA-06` | The initial Error state shall provide Retry, and Retry shall repeat the failed mode/page/page-size request. | Must | Fail once, select Retry, then verify the repeated request and result. |
| `FR-HIS-STA-07` | A refresh failure when existing data is visible shall retain that data and produce one failure effect. | Must | Load data, fail refresh, and verify data plus one message/effect. |
| `FR-HIS-STA-08` | A refresh success shall replace the displayed records and paging metadata with the refreshed result. | Must | Change repository data, refresh, and verify replacement. |
| `FR-HIS-STA-09` | Opening History and initial tab selection shall not trigger duplicate equivalent loads. | Must | Count requests during initial screen creation. |
| `FR-HIS-STA-10` | A completed request for an inactive mode, obsolete page, or obsolete page size shall not replace the active presentation. | Must | Complete an older request after a newer selection and verify active state. |
| `FR-HIS-STA-11` | If a page-change request fails while a successful page is visible, the system shall retain the previous records, restore the previous successful page number and paging metadata, and produce one failure effect. | Must | Load one page, fail a request for another page, and verify restored state plus one failure effect. |
| `FR-HIS-STA-12` | If successful Refresh makes the retained current page greater than the new total pages, the system shall load and display the new final valid page without changing the selected mode or page size. | Must | Start on page 3, reduce the refreshed result to two pages, and verify page 2 loads with the retained mode and size. |

### 5.4 Delete saved receipt

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-DEL-01` | Receipts mode shall provide a delete action for a saved receipt. | Must | Verify the action is available for a Receipt summary. |
| `FR-HIS-DEL-02` | Selecting delete shall request explicit confirmation before any persistent data or image is removed. | Must | Open confirmation and verify the repository has not been called. |
| `FR-HIS-DEL-03` | Cancelling deletion confirmation shall close the confirmation and shall not change stored or displayed data. | Must | Cancel and verify no delete call or list change. |
| `FR-HIS-DEL-04` | Confirming deletion shall request deletion of the selected Receipt ID through the domain boundary. | Must | Confirm and verify the selected ID reaches the delete use case. |
| `FR-HIS-DEL-05` | Successful deletion shall remove the Receipt, its Receipt Items, and their Item Discounts. | Must | Query all related records after confirmed deletion. |
| `FR-HIS-DEL-06` | After database deletion succeeds, the system shall attempt to delete the app-owned private receipt image; image-deletion failure shall not restore or fail the deleted Receipt. | Must | Simulate image-deletion failure and verify the Receipt remains deleted. |
| `FR-HIS-DEL-07` | Successful deletion shall remove the associated Store only when no remaining Receipt references it. | Must | Test both shared-Store and unused-Store cases. |
| `FR-HIS-DEL-08` | The system shall report deletion success after the required database deletion succeeds; private-image cleanup shall not block that success. | Must | Delay/fail each operation separately and verify success depends on the database outcome only. |
| `FR-HIS-DEL-09` | If required database deletion fails, the system shall not report success and shall retain or reload a truthful valid page state. | Must | Fail database deletion and verify failure feedback and record availability. |
| `FR-HIS-DEL-10` | The system shall not offer Undo or recovery after confirmed successful deletion. | Must | Complete deletion and verify no Undo action is shown. |

### 5.5 Edit and update saved receipt

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-EDT-01` | Receipt Detail shall provide an Edit action for the displayed saved receipt. | Must | Open a saved Receipt Detail and verify Edit targets that ID. |
| `FR-HIS-EDT-02` | Starting Edit shall load the current saved Receipt aggregate into the receipt edit experience. | Must | Compare all loaded editable and read-only values with stored data. |
| `FR-HIS-EDT-03` | The edit experience shall allow modification of chain, optional branch, and purchase date. | Must | Change and save each store/receipt field, including an empty Branch. |
| `FR-HIS-EDT-04` | The edit experience shall allow modification of item name, quantity, unit, unit price, and category. | Must | Change and save each approved item field. |
| `FR-HIS-EDT-05` | The user shall be able to add an item and remove an item before updating the receipt. | Must | Add/remove rows and verify the saved aggregate. |
| `FR-HIS-EDT-06` | Receipt validation for both initial save and update shall require a non-empty chain and at least one valid item; Branch shall be optional and may be empty. | Must | Save and update with an empty Branch, then attempt each truly required value missing. |
| `FR-HIS-EDT-07` | Every saved item shall require a non-empty item name, quantity greater than zero, and unit price greater than or equal to zero. | Must | Test every invalid boundary and a valid item. |
| `FR-HIS-EDT-08` | The user shall be able to select an available child category or Uncategorized for each item. | Must | Save categorized and uncategorized items. |
| `FR-HIS-EDT-09` | The edit experience shall recalculate and display the final payable total when editable item values change. | Must | Change item values and compare the displayed total with domain calculation. |
| `FR-HIS-EDT-10` | The recognised printed total shall remain read-only, and a difference greater than one cent from the calculated total shall remain visible as a warning rather than automatically blocking update. | Must | Test matching, mismatching, and unrecognised printed totals. |
| `FR-HIS-EDT-11` | Raw OCR text and receipt image shall remain read-only, and editing shall not run OCR or a supermarket parser again. | Must | Edit/update and verify source metadata and no OCR/parser invocation. |
| `FR-HIS-EDT-12` | Existing Item Discounts shall not be directly editable; discounts shall remain with retained items and shall be removed with a removed item. | Must | Retain and remove discounted items, then inspect saved results. |
| `FR-HIS-EDT-13` | An item added during editing shall initially have no Item Discounts. | Must | Add and save an item, then inspect discount records. |
| `FR-HIS-EDT-14` | Cancelling Edit through the explicit Cancel action shall return to Receipt Detail without changing persistent data. | Must | Modify fields, cancel, and compare stored data. |
| `FR-HIS-EDT-15` | Confirming a valid update shall persist the complete edited aggregate under the original Receipt ID and shall not create a duplicate Receipt. | Must | Compare Receipt IDs and total Receipt count before/after update. |
| `FR-HIS-EDT-16` | Updating the Receipt aggregate shall be atomic: partial Store, Receipt, Item, Discount, or category-reference changes shall not remain after failure. | Must | Inject a failure during update and verify rollback. |
| `FR-HIS-EDT-17` | If chain or branch changes, the system shall use the resulting Store and remove the previous Store only when no Receipt still references it. | Must | Test existing, new, shared, and unused Store cases. |
| `FR-HIS-EDT-18` | A successful update shall report success and return to Receipt Detail showing the updated Receipt. | Must | Complete update and verify navigation and refreshed values. |
| `FR-HIS-EDT-19` | A failed update shall remain in the edit experience, preserve the user's editable values, report failure once, and shall not report success. | Must | Fail update and inspect edit fields, effects, and stored data. |
| `FR-HIS-EDT-20` | If Android Back is requested after editable values have changed, the system shall ask the user to confirm whether to discard the unsaved changes before leaving Edit. | Must | Modify a field, press Back, and verify confirmation appears before navigation. |
| `FR-HIS-EDT-21` | From the unsaved-changes confirmation, choosing Keep Editing shall retain the current editable values, while choosing `Don't Save` shall return to Receipt Detail without changing persistent data. | Must | Exercise both confirmation actions and compare UI and stored data. |
| `FR-HIS-EDT-22` | Editing purchase date shall change only the calendar date and shall preserve the saved receipt's existing time-of-day value. | Must | Update the date and compare the stored date and time components before and after. |
| `FR-HIS-EDT-23` | Initial save and update shall be blocked when any item final subtotal or the Receipt final payable total is negative, and the system shall provide visible validation feedback. | Must | Produce each negative result and verify no persistence plus one visible validation outcome. |
| `FR-HIS-EDT-24` | When Chain is `null`, empty, or contains only whitespace, initial save and update shall be blocked and the Chain input area shall show red validation styling; Branch shall remain optional. | Must | Attempt initial save and update with each invalid Chain form and verify red Chain feedback, no persistence, and no Branch-required error. |

### 5.6 Duplicate receipt detection before initial save

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-DUP-01` | For duplicate comparison, the system shall normalize Chain by removing every character except ASCII letters and digits and converting letters to lowercase; display text shall not be replaced by the comparison value. | Must | Compare Chains with case, space, and punctuation differences, such as `PAK'nSAVE` and `PAKNSAVE`, and verify they match while display text remains approved. |
| `FR-HIS-DUP-02` | Before inserting a new Receipt, the system shall check whether an existing Receipt has the same normalized Chain, purchase calendar date and hour, and final payable total in cents; minutes, seconds, and fractional seconds shall not participate in the duplicate key. | Must | Test each key component and verify two timestamps within the same hour match while adjacent hours do not. |
| `FR-HIS-DUP-03` | If no matching Receipt exists, the system shall continue the initial save without displaying a duplicate confirmation. | Must | Save a unique Receipt and verify one insertion and no prompt. |
| `FR-HIS-DUP-04` | If a matching Receipt exists, the system shall not insert immediately and shall display title `Possible duplicate receipt`, message `Add anyway?`, and Discard/Add actions. | Must | Attempt a matching save and verify the exact title, message, actions, and zero insertion before a decision. |
| `FR-HIS-DUP-05` | Selecting Discard shall close the duplicate confirmation, perform no insertion, and retain the reviewed draft for correction or cancellation. | Must | Select Discard and verify Receipt count, current draft, and screen location. |
| `FR-HIS-DUP-06` | Selecting Add shall insert the new Receipt once even though the duplicate key matches an existing Receipt. | Must | Select Add and verify exactly one additional Receipt. |
| `FR-HIS-DUP-07` | Updating an existing Receipt under its original Receipt ID shall not invoke the duplicate-add confirmation against that same Receipt. | Must | Update without changing the duplicate key and verify no self-duplicate prompt or extra Receipt. |

### 5.7 Purchase timestamp capture and initial review

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-TIM-01` | When the receipt Parser recognises a valid purchase date and time, the system shall use that parsed value as the draft Receipt purchase timestamp. | Must | Parse a receipt fixture containing a known timestamp and verify the draft value. |
| `FR-HIS-TIM-02` | When the Parser does not provide a valid purchase date and time, the system shall use the local `LocalDateTime.now()` value recorded for image capture/selection as the draft fallback timestamp. | Must | Process a fixture without a timestamp using a controlled clock and verify the fallback value. |
| `FR-HIS-TIM-03` | Scanner Receipt Review shall display and allow the user to modify both purchase date and purchase time before initial save. | Must | Change date and time independently and verify the saved timestamp. |
| `FR-HIS-TIM-04` | The final reviewed timestamp shall be normalized to second precision before duplicate comparison and persistence. | Must | Enter or parse fractional seconds and verify they are stored as zero while the second remains. |

### 5.8 Store identity and reuse

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `FR-HIS-STR-01` | For Store identity comparison, the system shall normalize both Chain and Branch by removing every character except ASCII letters and digits and converting letters to lowercase. | Must | Compare Store inputs that differ by case, spaces, or punctuation and verify the same identity key. |
| `FR-HIS-STR-02` | A `null`, empty, whitespace-only, or non-alphanumeric-only Branch shall normalize to the same empty Branch identity value. | Must | Save and update using each Branch representation and verify one Store identity. |
| `FR-HIS-STR-03` | Initial save and update shall reuse an existing Store whose normalized Chain and Branch identity match instead of creating another Store. | Must | Save/update matching variants such as `Greenlane`, ` greenlane `, and `GREENLANE`, then verify one Store row is referenced. |
| `FR-HIS-STR-04` | Branch normalization for Store identity shall not add Branch to the duplicate-Receipt key, which remains normalized Chain, purchase date/hour, and final payable total. | Must | Use different Branch values with the same duplicate key and verify duplicate detection still occurs. |

## 6. Non-functional requirements

| ID | Requirement | Priority | Verification summary |
| --- | --- | --- | --- |
| `NFR-HIS-ARC-01` | History presentation shall depend on domain use cases/contracts and shall not access Room DAOs or repository implementations directly. | Must | Static code review/import check. |
| `NFR-HIS-ARC-02` | Shared receipt validation shall not require History to call Scanner Fragment or Scanner ViewModel logic. | Must | Static dependency and unit-test review. |
| `NFR-HIS-DAT-01` | History read, count, duplicate lookup, save, update, delete, and image operations shall execute off the main UI thread. | Must | Executor-based tests and strict-mode/manual verification. |
| `NFR-HIS-DAT-02` | Existing saved data created before History Completion v1 shall remain readable, editable, and deletable. | Must | Regression test using an existing database/fixture. |
| `NFR-HIS-DAT-03` | Monetary data shall remain stored and processed as integer cents; quantity may remain decimal. | Must | Model/schema and calculation tests. |
| `NFR-HIS-PRV-01` | Core History browse, edit, and delete operations shall require no account, backend, or network connection. | Must | Run acceptance flow offline. |
| `NFR-HIS-CMP-01` | History Completion v1 shall support the project's configured Android API range beginning at API 26. | Must | Build configuration review and selected-device test. |
| `NFR-HIS-USE-01` | History and Receipt Review user-facing labels, actions, errors, and confirmations introduced by this feature shall be defined in Android string resources. | Must | Resource/lint review; no new hard-coded user-facing feature text. |
| `NFR-HIS-REL-01` | Replayed lifecycle observations shall not repeat a previously consumed success, failure, duplicate confirmation, or navigation effect. | Must | ViewModel/lifecycle event tests. |
| `NFR-HIS-TST-01` | Each Must functional requirement shall trace to at least one automated test or manual acceptance test before implementation is accepted. | Must | Traceability review against `06-acceptance-tests.md`. |

No numeric response-time target is approved in scope. Performance thresholds
shall not be invented in implementation; a measurable target may be added by an
approved requirements revision if device testing identifies a need.

## 7. Explicit exclusions

The following are not requirements for History Completion v1:

- search, free-text query, date-range filter, Store filter, Category filter, or
  alternate sort order;
- deletion Undo or recovery;
- editing/replacing the receipt image;
- re-running OCR, layout reconstruction, chain detection, or receipt parsing;
- directly editing raw OCR text, printed total, or Item Discounts;
- Analytics UI or category-spending charts;
- cloud sync, accounts, backup, export, import, printing, or sharing;
- infinite scrolling;
- permanent persistence of History page/page-size preferences across an app
  restart;
- New World or Four Square parser support.

An excluded capability requires an approved scope and requirements change before
development.

## 8. Requirement-to-system impact preview

This preview supports feasibility review; it does not prescribe final class
design.

| Requirement area | Expected affected boundaries |
| --- | --- |
| Browse and state | History Fragment, adapters, History ViewModel, immutable History UI state/effects |
| Counts and direct paging | Paged domain result, repository contract/implementation, Receipt DAO count queries |
| Duplicate detection | Receipt Review effect/dialog, duplicate-check use case, repository contract, Room candidate lookup, and domain total comparison |
| Purchase timestamp | Supermarket Parsers, parsed-receipt model, Scanner ViewModel state, Receipt Review controls, shared clock/time boundary, and Room persistence |
| Store identity | Shared normalization policy, save/update use cases, repository mapping, Store lookup query, and unused-Store cleanup |
| Deletion | History UI confirmation/effects, Delete Receipt use case, repository transaction, and best-effort image cleanup |
| Edit entry | Receipt Detail UI/navigation and receipt-loading state |
| Receipt update | Shared review/edit UI, validation boundary, Update Receipt use case, repository/DAO transaction |
| Verification | ViewModel/domain unit tests, Room integration tests, manual acceptance tests |

## 9. Resolved requirement decisions

The Systems Analyst/Product Owner approved the following decisions on
2026-08-17. They are incorporated into the requirements identified below and
are no longer open questions.

| ID | Approved decision | Requirement impact |
| --- | --- | --- |
| `OQR-HIS-01` | Branch is optional when initially saving and when editing a receipt. | Revised `FR-HIS-EDT-03` and `FR-HIS-EDT-06`. |
| `OQR-HIS-02` | Editing purchase date changes the calendar date only and preserves the existing time. | Added `FR-HIS-EDT-22`. |
| `OQR-HIS-03` | Android Back with unsaved changes displays a discard-changes confirmation. | Added `FR-HIS-EDT-20` and `FR-HIS-EDT-21`. |
| `OQR-HIS-04` | A failed page change retains and restores the previous successful page and displays one failure effect. | Added `FR-HIS-STA-11`. |
| `OQR-HIS-05` | Zero records are represented as `Page 1 of 1`; the first page contains zero records and no other page is selectable. | Revised the Valid page definition and added `FR-HIS-PAG-13`. |
| `OQR-HIS-06` | Receipts with the same purchase date and time are displayed FIFO by saved order. | Added `FR-HIS-BRW-09`; stable saved-order persistence is required. |
| `OQR-HIS-07` | Negative item final subtotals and negative Receipt final payable totals are invalid and block save/update. | Added `FR-HIS-EDT-23`. |
| `OQR-HIS-08` | Database deletion determines delete success; private-image deletion is best-effort and failure is deferred to future cleanup. | Revised `FR-HIS-DEL-06`, `FR-HIS-DEL-08`, and `FR-HIS-DEL-09`. |
| `OQR-HIS-09` | Chain is required and invalid when `null`, empty, or whitespace-only; invalid Chain uses red input-area feedback, while Branch remains optional. | Added `FR-HIS-EDT-24`; exact presentation will be defined in `05-ui-specification.md`. |
| `OQR-HIS-10` | A potential duplicate uses normalized Chain, purchase calendar date/hour, and final payable total; minutes and seconds are ignored, Add inserts, and Discard performs no insertion while retaining Review. | Added `FR-HIS-DUP-01` through `FR-HIS-DUP-07`. |
| `OQR-HIS-11` | Refresh falls back to the new final valid page when the retained page no longer exists. | Added `FR-HIS-STA-12`. |
| `OQR-HIS-12` | Chain comparison retains ASCII letters and digits only and lowercases letters without changing display text. | Added `FR-HIS-DUP-01`. |
| `OQR-HIS-13` | History retains its current `yyyy-MM-dd HH:mm` timestamp presentation, while timestamps at second precision remain stored and control ordering. | Revised `FR-HIS-BRW-05` and `FR-HIS-BRW-10`. |
| `OQR-HIS-14` | Parser-recognised receipt purchase date/time is preferred; when absent, capture-time local `LocalDateTime.now()` supplies the fallback. | Added `FR-HIS-TIM-01`, `FR-HIS-TIM-02`, and `FR-HIS-TIM-04`. |
| `OQR-HIS-15` | Scanner Receipt Review allows the user to modify purchase date and time before initial save. | Added `FR-HIS-TIM-03`. |
| `OQR-HIS-16` | Store identity uses lowercase ASCII-alphanumeric-only Chain and Branch keys; `null`, empty, whitespace-only, and non-alphanumeric-only Branch are equivalent. | Added `FR-HIS-STR-01` through `FR-HIS-STR-04`. |

## 10. Requirements approval checklist

The document can move from Draft to Approved when:

- all requirements are necessary, testable, and consistent with Scope v0.9;
- resolved decisions `OQR-HIS-01` through `OQR-HIS-16` are reflected in the
  requirements, use cases, business rules, UI specification, and tests;
- no requirement silently introduces an excluded capability;
- developer review confirms technical feasibility without changing user intent;
- tester review confirms the requirements can produce unambiguous acceptance
  evidence;
- later documents preserve these requirement IDs for traceability.

## 11. Approval

| Role | Name | Decision | Date |
| --- | --- | --- | --- |
| Product owner | Victor Shih | Approved for use-case analysis | 2026-08-17 |
| Systems analyst | Victor Shih | Requirements revised after clarification | 2026-08-17 |
| Developer |  | Pending technical review |  |
| Tester |  | Pending testability review |  |
