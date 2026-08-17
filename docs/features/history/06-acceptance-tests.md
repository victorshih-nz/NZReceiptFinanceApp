# History Acceptance Tests

## 1. Document control

| Field | Value |
| --- | --- |
| Feature ID | `HIS` |
| Feature name | History Completion v1 |
| Document ID | `HIS-ATS-06` |
| Repository source | `06-acceptance-tests.md` |
| Version | 0.2 |
| Status | Draft for review |
| Date | 2026-08-17 |
| Author | Victor Shih — Systems Analyst |
| Source scope | `HIS-SCP-01`, version 0.9 |
| Source requirements | `HIS-FRS-02`, version 1.0 |
| Source use cases | `HIS-UCS-03`, version 0.9 |
| Source business rules | `HIS-BRS-04`, version 0.8 |
| Source UI specification | `HIS-UIS-05`, version 0.5 |
| Code baseline | `main` at `5fd0fbd` |

### 1.1 Revision history

| Version | Date | Author | Change |
| --- | --- | --- | --- |
| 0.1 | 2026-08-17 | Victor Shih | Initial acceptance-test specification covering History browse, paging, state, delete, saved-Receipt edit, timestamp, duplicate detection, Store identity, and non-functional requirements |
| 0.2 | 2026-08-17 | Victor Shih | Added acceptance coverage for removing spaces and punctuation from normalized Chain and Branch keys |

## 2. Purpose

This document defines the evidence required to accept History Completion v1.
It translates the approved scope, functional requirements, use cases, business
rules, and UI specification into testable Given/When/Then scenarios.

The document is implementation-independent. Class and method names may change
during refactoring, but the observable behaviour and requirement traceability
must remain valid.

## 3. Acceptance approach

### 3.1 Test levels

| Level | Purpose | Typical implementation |
| --- | --- | --- |
| Domain unit | Verify calculation, validation, normalization, timestamp, duplicate-key, and use-case rules without Android UI. | JUnit tests with fakes or mocks. |
| ViewModel unit | Verify unified UI state, retained paging, stale-request protection, and one-time effects. | JUnit with controlled executors/data sources. |
| Room integration | Verify queries, ordering, transactions, cascades, Store reuse, and rollback. | Android instrumented test using an isolated Room database. |
| UI integration | Verify Fragment navigation, controls, dialogs, Snackbar, field errors, and displayed values. | Espresso or equivalent instrumented tests. |
| Manual UI | Verify usability, accessibility, layout, device compatibility, and behaviours that are expensive to automate initially. | Android Studio emulator or supported physical device. |
| Static review | Verify architecture boundaries, string resources, Gradle configuration, and schema/model constraints. | Code review, lint, and dependency inspection. |

Automated tests are preferred for deterministic business rules. A manual test
may temporarily satisfy an acceptance criterion only when its result and
evidence are recorded and the traceability table identifies it.

### 3.2 Given/When/Then convention

- **Given** defines data, screen state, and controlled dependency behaviour.
- **When** defines one user action or system event.
- **Then** defines externally observable results and required stored state.
- Multiple **And** statements are all required for the scenario to pass.
- Unless explicitly stated otherwise, each scenario begins with a clean app
  process and an isolated test database.

### 3.3 Result and evidence

For each executed scenario, record:

| Field | Required value |
| --- | --- |
| Build | Commit SHA and build variant. |
| Environment | Unit JVM, emulator/device model, Android API, and app version. |
| Result | Pass, Fail, or Blocked. |
| Evidence | Test report path, screenshot, screen recording, database assertion, or code-review reference. |
| Defect | Issue ID when the result is Fail or Blocked. |
| Tester and date | Person and execution date. |

## 4. Test environment and reference data

### 4.1 Required environments

- JVM unit-test environment used by `testDebugUnitTest`.
- Isolated Android instrumented-test database; production user data must not be
  changed by automated tests.
- At least one emulator or physical device on API 26.
- At least one emulator on the project's current target API.
- Offline mode for the no-network acceptance scenario.
- A controllable clock and controllable executor for timestamp and asynchronous
  state tests.

### 4.2 Receipt fixtures

All monetary values below are integer cents in storage.

| Fixture | Chain | Branch | Purchase timestamp | Final payable | Notes |
| --- | --- | --- | --- | ---: | --- |
| `R-A` | `Woolworths` | `Greenlane` | `2026-08-17 10:05:12` | 9500 | Two valid items; printed total 9500. |
| `R-B` | `PAK'nSAVE` | `Royal Oak` | `2026-08-17 09:59:59` | 4210 | One categorized and one uncategorized item. |
| `R-C` | `New World` | empty | `2026-08-16 18:30:45` | 1835 | Optional Branch example. |
| `R-D1` | `Woolworths` | `Greenlane` | `2026-08-15 08:00:00` | 1200 | Saved before `R-D2`. |
| `R-D2` | `Woolworths` | `Greenlane` | `2026-08-15 08:00:00` | 1300 | Same timestamp as `R-D1`; FIFO check. |
| `R-SHARED-1` | `Woolworths` | `Mt Eden` | `2026-08-14 10:00:00` | 1000 | Shares one Store row with `R-SHARED-2`. |
| `R-SHARED-2` | ` woolworths ` | ` mt eden ` | `2026-08-14 11:00:00` | 1500 | Store-normalization variant. |

### 4.3 Duplicate fixtures

Assume `R-A` already exists.

| Draft | Chain | Branch | Purchase timestamp | Final payable | Expected |
| --- | --- | --- | --- | ---: | --- |
| `D-SAME` | ` wool-worths! ` | `Mt Eden` | `2026-08-17 10:59:59` | 9500 | Duplicate; spaces and punctuation are removed from the same normalized Chain. |
| `D-NEXT-HOUR` | `WOOLWORTHS` | `Greenlane` | `2026-08-17 11:00:00` | 9500 | Unique; different hour. |
| `D-NEXT-DAY` | `Woolworths` | `Greenlane` | `2026-08-18 10:05:12` | 9500 | Unique; different date. |
| `D-TOTAL` | `Woolworths` | `Greenlane` | `2026-08-17 10:05:12` | 9501 | Unique; different total in cents. |
| `D-CHAIN` | `New World` | `Greenlane` | `2026-08-17 10:05:12` | 9500 | Unique; different normalized Chain. |

### 4.4 Paging fixtures

- A 0-record data set for Empty behaviour.
- A 31-Receipt data set for Receipt pages of 15, 30, and 50.
- A 61-item data set for All Items pages of 15, 30, and 50.
- Records with timestamps differing only by seconds.
- Records spanning page boundaries, including equal purchase timestamps whose
  insertion order is known.

### 4.5 OCR and edit fixtures

- `OCR-WITH-TIME`: parser result contains a valid known purchase date and time.
- `OCR-WITHOUT-TIME`: parser result contains no valid purchase timestamp.
- One Receipt with retained Item Discounts and a read-only image/raw OCR value.
- One Receipt whose printed total differs from the calculated total by one cent,
  and one whose difference is greater than one cent.
- Invalid edit inputs: blank Chain, whitespace Chain, no items, blank item name,
  quantity zero, quantity below zero, unit price below zero, item negative final
  subtotal, and Receipt negative final payable total.

## 5. Acceptance scenarios

### 5.1 History browse, ordering, and paging

#### `AT-HIS-001` — Open History with Receipt content

- **Level:** ViewModel unit + Room integration + UI integration
- **Requirements:** `FR-HIS-BRW-01`, `FR-HIS-BRW-02`, `FR-HIS-BRW-05`,
  `FR-HIS-BRW-09`, `FR-HIS-BRW-10`, `FR-HIS-PAG-01`,
  `FR-HIS-PAG-06`, `FR-HIS-STA-01`, `FR-HIS-STA-02`,
  `FR-HIS-STA-03`
- **Given** History has no retained state and the database contains the Receipt
  fixtures, including `R-D1` and `R-D2`.
- **When** the user opens History.
- **Then** Receipts is selected and the initial state progresses from Loading to
  Content.
- **And** page size is 15 and page metadata matches the known record count.
- **And** Receipts are ordered by full purchase timestamp, newest first.
- **And** `R-D1` appears before `R-D2` because it was saved first.
- **And** each card displays Chain, optional Branch, `yyyy-MM-dd HH:mm`, item
  count, and final payable total.
- **And** stored purchase timestamps retain seconds with fractional seconds zero.

#### `AT-HIS-002` — Empty Receipts result

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-PAG-13`, `FR-HIS-STA-01`, `FR-HIS-STA-04`
- **Given** the Receipt query succeeds with zero records.
- **When** History finishes its initial load.
- **Then** the Receipt Empty state is displayed without an Error state.
- **And** paging displays `Page 1 of 1`.
- **And** Previous, Next, and selection of another page are disabled.

#### `AT-HIS-003` — Switch modes and retain independent paging

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-BRW-03`, `FR-HIS-PAG-02`,
  `FR-HIS-PAG-05`
- **Given** Receipts is on page 2 with page size 15.
- **When** the user selects All Items for the first time.
- **Then** All Items loads with default page size 30 and its own page 1.
- **When** the user changes All Items to page 2 with page size 50 and switches
  between the modes.
- **Then** Receipts restores page 2/size 15 and All Items restores page 2/size
  50 while the History state remains active.
- **And** records from the inactive mode are never presented under the selected
  tab.

#### `AT-HIS-004` — Display and order All Items

- **Level:** Room integration + UI integration
- **Requirements:** `FR-HIS-BRW-04`, `FR-HIS-BRW-06`,
  `FR-HIS-PAG-06`, `FR-HIS-PAG-13`
- **Given** the database contains categorized and uncategorized items belonging
  to Receipts on different purchase dates.
- **When** the user selects All Items.
- **Then** items are ordered by newest parent-Receipt purchase date first.
- **And** each card displays item name, Chain, `yyyy-MM-dd`, category or
  `Uncategorized`, and final subtotal.
- **And** total records, pages, current page, and page size are correct.
- **And** the zero-item variant displays Empty and `Page 1 of 1`.

#### `AT-HIS-005` — Select page size

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-PAG-03`, `FR-HIS-PAG-04`
- **Given** either mode is on a page later than page 1.
- **When** the user selects each supported size: 15, 30, and 50.
- **Then** only the active mode resets to page 1.
- **And** its query limit and resulting page metadata use the selected size.
- **And** the inactive mode's retained page and size do not change.

#### `AT-HIS-006` — Select a page directly

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-PAG-07`, `FR-HIS-PAG-08`
- **Given** the active mode has at least three pages.
- **When** the user opens the page Dropdown.
- **Then** it contains only pages 1 through the current total pages.
- **When** the user selects the first, a middle, and the final page.
- **Then** each selected page loads the correct records.
- **And** a page below 1 or above total pages cannot be selected or queried.

#### `AT-HIS-007` — Previous and Next boundaries

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-PAG-09`, `FR-HIS-PAG-10`
- **Given** the active mode has multiple pages.
- **Then** Previous is disabled on page 1 and Next is enabled.
- **When** Next is selected.
- **Then** the next page loads.
- **When** Previous is selected from a later page.
- **Then** the preceding page loads.
- **And** Next is disabled on the final page.

#### `AT-HIS-008` — Ignore duplicate and stale asynchronous results

- **Level:** ViewModel unit
- **Requirements:** `FR-HIS-PAG-11`, `FR-HIS-STA-09`,
  `FR-HIS-STA-10`
- **Given** data-source completion order can be controlled.
- **When** History is created and its initial tab is selected.
- **Then** only one equivalent initial load is requested.
- **When** an older request completes after the user has selected another mode,
  page, or page size.
- **Then** the obsolete result does not replace the current state or paging
  controls.

### 5.2 Loading, retry, refresh, and page recovery

#### `AT-HIS-009` — Initial failure and Retry

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-STA-01`, `FR-HIS-STA-02`,
  `FR-HIS-STA-05`, `FR-HIS-STA-06`
- **Given** the initial request fails and no successful content exists.
- **When** the failure completes.
- **Then** History displays an inline Error, not Empty, and offers Retry.
- **When** Retry is selected and the data source succeeds.
- **Then** the same mode, page, and page-size request is repeated and Content is
  displayed.

#### `AT-HIS-010` — Successful pull-to-refresh

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-BRW-08`, `FR-HIS-STA-08`
- **Given** a successful page is visible with a non-default page and page size.
- **And** the repository data and count have changed.
- **When** the user pulls to refresh.
- **Then** the selected mode and page size are retained.
- **And** displayed records and paging metadata are replaced by the successful
  refreshed result.

#### `AT-HIS-011` — Failed refresh preserves content

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-BRW-08`, `FR-HIS-STA-07`
- **Given** a successful page is visible.
- **When** pull-to-refresh fails.
- **Then** the last successful records and paging metadata remain visible.
- **And** the refresh indicator ends.
- **And** exactly one failure Snackbar is shown.
- **And** the screen does not change to initial Error or Empty.

#### `AT-HIS-012` — Refresh falls back to the last valid page

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-STA-08`, `FR-HIS-STA-12`
- **Given** the selected mode is on page 3 and Refresh reduces the result to two
  pages.
- **When** Refresh obtains the new count.
- **Then** the system loads page 2, the new final valid page.
- **And** the selected mode and page size remain unchanged.
- **And** page text, Dropdown, Previous, and Next represent page 2 of 2.

#### `AT-HIS-013` — Failed page change restores the successful page

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-PAG-11`, `FR-HIS-STA-11`
- **Given** page 1 has loaded successfully.
- **When** a request for page 2 fails.
- **Then** page 1 records, counts, page number, Dropdown value, and button states
  are restored.
- **And** exactly one failure Snackbar is shown.

### 5.3 Receipt Detail and deletion

#### `AT-HIS-014` — Open the selected Receipt Detail

- **Level:** UI integration
- **Requirements:** `FR-HIS-BRW-07`, `FR-HIS-EDT-01`
- **Given** Receipt cards for different stable IDs are visible.
- **When** the user selects one card.
- **Then** Receipt Detail opens for exactly that Receipt ID.
- **And** it shows the matching aggregate and provides an Edit action.

#### `AT-HIS-015` — Cancel deletion

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-DEL-01`, `FR-HIS-DEL-02`,
  `FR-HIS-DEL-03`
- **Given** a Receipt card is visible with a Delete action.
- **When** the user selects Delete.
- **Then** explicit confirmation appears and no deletion has occurred.
- **When** the user cancels.
- **Then** the dialog closes, stored data is unchanged, and the card remains.

#### `AT-HIS-016` — Delete a Receipt and its owned records

- **Level:** Domain unit + Room integration + UI integration
- **Requirements:** `FR-HIS-DEL-04`, `FR-HIS-DEL-05`,
  `FR-HIS-DEL-06`, `FR-HIS-DEL-07`, `FR-HIS-DEL-08`
- **Given** a Receipt has Items, Item Discounts, an app-owned private image, and
  a Store referenced by no other Receipt.
- **When** the user confirms deletion.
- **Then** the selected ID is passed through the domain deletion boundary.
- **And** the Receipt, its Items, and their Discounts are removed atomically.
- **And** the now-unused Store is removed.
- **And** private-image deletion is attempted after database success.
- **And** one success result is shown and the visible Receipt page is reloaded.

#### `AT-HIS-017` — Preserve a shared Store during deletion

- **Level:** Room integration
- **Requirements:** `FR-HIS-DEL-07`
- **Given** `R-SHARED-1` and `R-SHARED-2` reference the same normalized Store.
- **When** one Receipt is deleted successfully.
- **Then** the Store remains and the other Receipt still references it.

#### `AT-HIS-018` — Image cleanup failure does not fail deletion

- **Level:** Domain unit + Room integration
- **Requirements:** `FR-HIS-DEL-06`, `FR-HIS-DEL-08`
- **Given** database deletion succeeds and private-image deletion fails.
- **When** deletion completes.
- **Then** the Receipt remains deleted and is not restored.
- **And** deletion is reported as successful without Undo.

#### `AT-HIS-019` — Database deletion failure

- **Level:** Domain unit + ViewModel unit
- **Requirements:** `FR-HIS-DEL-09`
- **Given** required database deletion fails.
- **When** the user confirms deletion.
- **Then** success is not reported.
- **And** the selected Receipt remains stored.
- **And** the UI retains or reloads a truthful valid Receipt page and shows one
  failure Snackbar.

#### `AT-HIS-020` — Reposition after deleting the final record on a page

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-PAG-12`, `FR-HIS-DEL-05`
- **Given** a non-first Receipt page contains only one Receipt.
- **When** that Receipt is deleted successfully.
- **Then** the previous valid page is loaded.
- **And** if the deletion leaves zero Receipts, History displays Empty with
  `Page 1 of 1`.

#### `AT-HIS-021` — No deletion Undo

- **Level:** UI integration
- **Requirements:** `FR-HIS-DEL-10`
- **Given** a Receipt deletion has succeeded.
- **When** success feedback is displayed.
- **Then** no Undo, Restore, or recovery action is offered.

### 5.4 Edit a saved Receipt

#### `AT-HIS-022` — Open Edit with the saved aggregate

- **Level:** ViewModel unit + UI integration
- **Requirements:** `FR-HIS-EDT-01`, `FR-HIS-EDT-02`,
  `FR-HIS-EDT-03`, `FR-HIS-EDT-04`
- **Given** Receipt Detail shows a saved Receipt.
- **When** the user selects Edit.
- **Then** Edit loads that Receipt's Chain, optional Branch, purchase date,
  items, quantity, unit, unit price, category, totals, discounts, image, and raw
  OCR values.
- **And** only the fields approved by the UI specification are editable.
- **And** image and raw OCR values remain read-only, and opening or saving Edit
  does not invoke OCR or a supermarket Parser again.

#### `AT-HIS-023` — Validate required Chain and optional Branch

- **Level:** Domain unit + UI integration
- **Requirements:** `FR-HIS-EDT-06`, `FR-HIS-EDT-24`
- **Given** a valid Receipt draft exists.
- **When** initial save or update is attempted with Chain `null`, empty, or only
  whitespace.
- **Then** persistence is blocked, focus moves to Chain, and the Chain area has
  red styling plus readable error text.
- **When** Chain is valid and Branch is `null`, empty, or whitespace.
- **Then** Branch produces no required-field error and the Receipt may be saved
  if all other inputs are valid.

#### `AT-HIS-024` — Validate required item values

- **Level:** Domain unit + UI integration
- **Requirements:** `FR-HIS-EDT-06`, `FR-HIS-EDT-07`
- **Given** the user is performing initial save or update.
- **When** there are no items, or an item has blank name, quantity at or below
  zero, or unit price below zero.
- **Then** persistence is blocked and readable field-level feedback identifies
  the first invalid value.
- **When** an item has a name, quantity above zero, and unit price zero or above.
- **Then** that item passes these validation rules.

#### `AT-HIS-025` — Add, remove, categorize, and retain discounts

- **Level:** Domain unit + Room integration + UI integration
- **Requirements:** `FR-HIS-EDT-05`, `FR-HIS-EDT-08`,
  `FR-HIS-EDT-12`, `FR-HIS-EDT-13`
- **Given** a saved Receipt contains categorized, uncategorized, discounted, and
  non-discounted items.
- **When** the user retains one discounted item, removes another discounted
  item, adds an item, and selects a child category or Uncategorized.
- **Then** retained-item Discounts remain unchanged.
- **And** removed-item Discounts are removed with the item.
- **And** the new item initially has no Discounts.
- **And** the selected categories are saved.

#### `AT-HIS-026` — Recalculate totals and show printed-total mismatch

- **Level:** Domain unit + UI integration
- **Requirements:** `FR-HIS-EDT-09`, `FR-HIS-EDT-10`,
  `NFR-HIS-DAT-03`
- **Given** editable items and an optional recognised printed total are visible.
- **When** quantity, unit price, item membership, or discount-affecting retained
  data changes.
- **Then** item subtotals and Receipt final payable total are recalculated using
  integer cents.
- **And** printed total remains read-only.
- **And** a difference greater than one cent is visibly warned but does not by
  itself block update.
- **And** a difference of zero or one cent does not show the greater-than-one-
  cent warning.

#### `AT-HIS-027` — Block negative payable values

- **Level:** Domain unit + UI integration
- **Requirements:** `FR-HIS-EDT-23`, `NFR-HIS-DAT-03`
- **Given** discounts or edited values produce a negative item final subtotal or
  negative Receipt final payable total.
- **When** the user attempts initial save or update.
- **Then** persistence is blocked.
- **And** one visible validation outcome identifies the negative-value problem.
- **And** no partially saved aggregate exists.

#### `AT-HIS-028` — Edit purchase date while preserving time

- **Level:** Domain unit + Room integration + UI integration
- **Requirements:** `FR-HIS-EDT-03`, `FR-HIS-EDT-22`
- **Given** a saved Receipt timestamp is `2026-08-17 10:05:12`.
- **When** the History Edit date is changed to `2026-08-20` and saved.
- **Then** the stored timestamp becomes `2026-08-20 10:05:12`.
- **And** History continues to display it as `2026-08-20 10:05`.

#### `AT-HIS-029` — Update atomically under the original Receipt ID

- **Level:** Domain unit + Room integration
- **Requirements:** `FR-HIS-EDT-15`, `FR-HIS-EDT-16`,
  `FR-HIS-EDT-18`, `FR-HIS-DUP-07`
- **Given** one saved Receipt aggregate is being edited.
- **When** the user confirms a valid update.
- **Then** all approved changes are committed atomically under the original
  Receipt ID.
- **And** Receipt count does not increase.
- **And** no duplicate-add confirmation is shown against the same Receipt.
- **And** success occurs once and Receipt Detail displays refreshed values.

#### `AT-HIS-030` — Reuse normalized Store during update

- **Level:** Domain unit + Room integration
- **Requirements:** `FR-HIS-EDT-17`, `FR-HIS-STR-01`,
  `FR-HIS-STR-03`
- **Given** Store `Woolworths / Greenlane` already exists.
- **When** a Receipt is saved or updated with ` wool-worths ` and ` GREEN LANE! `.
- **Then** the existing normalized Store is reused and no duplicate Store row is
  created.
- **And** if an edit moves the Receipt to another Store, the previous Store is
  removed only when no Receipt references it.

#### `AT-HIS-031` — Failed update rolls back and preserves draft

- **Level:** Room integration + ViewModel unit + UI integration
- **Requirements:** `FR-HIS-EDT-16`, `FR-HIS-EDT-19`
- **Given** the user has valid unsaved edits and a failure is injected during the
  aggregate transaction.
- **When** update is attempted.
- **Then** no partial Store, Receipt, Item, Discount, or category-reference
  changes remain.
- **And** Edit stays open with the user's values intact.
- **And** failure is reported once and success is not reported.

#### `AT-HIS-032` — Cancel Edit

- **Level:** UI integration + Room integration
- **Requirements:** `FR-HIS-EDT-14`
- **Given** the user has modified Edit fields.
- **When** the explicit Cancel action is selected.
- **Then** Receipt Detail is shown and persistent data is unchanged.

#### `AT-HIS-033` — Back with no changes

- **Level:** UI integration
- **Requirements:** `FR-HIS-EDT-20`
- **Given** Edit contains no changes from the loaded aggregate.
- **When** Android Back is requested.
- **Then** the screen returns to Receipt Detail without an unsaved-changes
  confirmation.

#### `AT-HIS-034` — Back and Keep Editing

- **Level:** UI integration
- **Requirements:** `FR-HIS-EDT-20`, `FR-HIS-EDT-21`
- **Given** at least one editable value has changed.
- **When** Android Back is requested and the user selects Keep Editing.
- **Then** Edit remains visible with all current editable values retained.
- **And** persistent data is unchanged.

#### `AT-HIS-035` — Back and Don't Save

- **Level:** UI integration + Room integration
- **Requirements:** `FR-HIS-EDT-20`, `FR-HIS-EDT-21`
- **Given** at least one editable value has changed.
- **When** Android Back is requested and the user selects `Don't Save`.
- **Then** Receipt Detail is shown and all unsaved changes are discarded.
- **And** persistent data is unchanged.

### 5.5 Receipt timestamp, duplicate detection, and Store identity

#### `AT-HIS-036` — Prefer a parsed purchase timestamp

- **Level:** Domain unit + parser integration
- **Requirements:** `FR-HIS-BRW-10`, `FR-HIS-TIM-01`
- **Given** `OCR-WITH-TIME` contains a valid known purchase date and time.
- **When** OCR text is parsed into the Scanner Review draft.
- **Then** the parsed purchase date and time are used instead of the image
  capture/selection time.
- **And** the draft preserves hour, minute, and second.

#### `AT-HIS-037` — Use capture/selection time as fallback

- **Level:** Domain/ViewModel unit
- **Requirements:** `FR-HIS-BRW-10`, `FR-HIS-TIM-02`
- **Given** the clock records image capture/selection at
  `2026-08-17 14:22:31.987` and `OCR-WITHOUT-TIME` has no valid timestamp.
- **When** the Scanner Review draft is produced after OCR completes.
- **Then** its timestamp is `2026-08-17 14:22:31`.
- **And** OCR completion time is not substituted for the earlier recorded
  capture/selection time.

#### `AT-HIS-038` — Review and edit date, time, and seconds

- **Level:** ViewModel unit + UI integration + Room integration
- **Requirements:** `FR-HIS-BRW-10`, `FR-HIS-TIM-03`,
  `FR-HIS-TIM-04`
- **Given** Scanner Receipt Review contains a valid draft timestamp.
- **When** the user changes the date, changes hour/minute through the TimePicker,
  and enters seconds in the separate seconds field.
- **Then** time is displayed as `HH:mm:ss` and the seconds field accepts only
  00 through 59.
- **And** changing only hour/minute preserves the existing seconds.
- **And** the final reviewed timestamp is normalized to second precision before
  comparison and persistence.

#### `AT-HIS-039` — Save a unique Receipt without a prompt

- **Level:** Domain unit + Room integration + UI integration
- **Requirements:** `FR-HIS-DUP-03`
- **Given** a valid new draft matches no existing duplicate key.
- **When** the user confirms save.
- **Then** no duplicate dialog is displayed.
- **And** exactly one Receipt aggregate is inserted and success occurs once.

#### `AT-HIS-040` — Discard a possible duplicate

- **Level:** Domain unit + ViewModel unit + UI integration
- **Requirements:** `FR-HIS-DUP-04`, `FR-HIS-DUP-05`
- **Given** `R-A` exists and the reviewed draft is `D-SAME`.
- **When** the user confirms save.
- **Then** no insertion occurs before the user's decision.
- **And** a dialog displays exactly:
  - title `Possible duplicate receipt`;
  - message `Add anyway?`;
  - actions `Discard` and `Add`.
- **When** the user selects Discard.
- **Then** the dialog closes, no Receipt is inserted, and Scanner Receipt Review
  remains open with the draft retained for correction or cancellation.
- **And** Discard does not delete any existing Receipt.

#### `AT-HIS-041` — Add a possible duplicate once

- **Level:** Domain unit + ViewModel unit + Room integration + UI integration
- **Requirements:** `FR-HIS-DUP-04`, `FR-HIS-DUP-06`
- **Given** `R-A` exists, the reviewed draft is `D-SAME`, and the duplicate dialog
  is visible.
- **When** the user selects Add once.
- **Then** exactly one additional Receipt aggregate is inserted.
- **And** repeated observation, rotation, or navigation does not insert it again
  or reopen the consumed dialog.

#### `AT-HIS-042` — Duplicate hour boundaries

- **Level:** Domain unit + Room integration
- **Requirements:** `FR-HIS-DUP-02`
- **Given** `R-A` exists at `2026-08-17 10:05:12` for 9500 cents.
- **When** `D-SAME` at `10:59:59` is checked.
- **Then** it is a duplicate because minutes and seconds are ignored.
- **When** `D-NEXT-HOUR` at `11:00:00` or `D-NEXT-DAY` is checked.
- **Then** each is unique because hour or calendar date differs.
- **And** a fractional-second difference does not affect the key.

#### `AT-HIS-043` — Normalize Chain and ignore Branch in duplicate matching

- **Level:** Domain unit + Room integration
- **Requirements:** `FR-HIS-DUP-01`, `FR-HIS-DUP-02`,
  `FR-HIS-STR-04`
- **Given** `R-A` exists.
- **When** `D-SAME` is checked using different Chain case, spaces, punctuation,
  and a different Branch.
- **Then** it is a duplicate.
- **And** the user's presentation casing is not replaced by the normalized key.
- **When** `D-TOTAL` or `D-CHAIN` is checked.
- **Then** it is unique because total or normalized Chain differs.

#### `AT-HIS-044` — Normalize optional Branch for Store identity

- **Level:** Domain unit + Room integration
- **Requirements:** `FR-HIS-STR-01`, `FR-HIS-STR-02`,
  `FR-HIS-STR-03`
- **Given** a normalized Store identity can be inspected.
- **When** otherwise equivalent Receipts use Branch `null`, empty, only
  whitespace, or punctuation only.
- **Then** all four forms identify the same empty-Branch Store.
- **When** Chain or Branch differs only by case, spaces, or punctuation.
- **Then** the existing matching Store is reused rather than duplicated.

### 5.6 Non-functional and regression acceptance

#### `AT-HIS-045` — Complete core History flow offline

- **Level:** Manual UI
- **Requirements:** `NFR-HIS-PRV-01`
- **Given** the device has airplane mode enabled and no account is signed in.
- **When** the user browses, pages, opens, edits, and deletes local Receipts.
- **Then** each core operation works without a backend or network connection.

#### `AT-HIS-046` — Do not replay consumed effects

- **Level:** ViewModel unit + UI integration
- **Requirements:** `NFR-HIS-REL-01`
- **Given** a success, failure, duplicate confirmation, or navigation effect has
  been handled once.
- **When** the Fragment view is recreated or its observer reattaches.
- **Then** the consumed effect is not emitted or acted on again.
- **And** a pending duplicate decision remains available only until the user
  chooses Discard or Add.

#### `AT-HIS-047` — Resources, accessibility, layout, and API support

- **Level:** Static review + lint + Manual UI
- **Requirements:** `NFR-HIS-CMP-01`, `NFR-HIS-USE-01`
- **Given** the feature build and layouts are available.
- **When** lint/static review and manual checks run on API 26 and the current
  target API using a small-phone portrait layout.
- **Then** new user-facing labels, actions, errors, and confirmations come from
  Android string resources.
- **And** interactive controls have at least 48 dp targets, icon actions have
  content descriptions, errors include text as well as colour, traversal is
  logical, contrast is readable, and bottom actions are not clipped.

#### `AT-HIS-048` — Existing database compatibility

- **Level:** Room migration/regression integration + Manual UI
- **Requirements:** `NFR-HIS-DAT-02`, `NFR-HIS-DAT-03`
- **Given** a database fixture created by the pre-feature schema contains saved
  Receipts, Items, Discounts, Stores, categories, images, and OCR text.
- **When** the upgraded app opens the database.
- **Then** existing data remains readable, editable, and deletable.
- **And** monetary values remain integer cents without value drift.

#### `AT-HIS-049` — Keep data and image work off the main thread

- **Level:** Unit/instrumented performance assertion + Manual StrictMode check
- **Requirements:** `NFR-HIS-DAT-01`
- **Given** StrictMode or an equivalent thread assertion is active.
- **When** read, count, duplicate lookup, save, update, delete, and image
  operations execute.
- **Then** none performs blocking work on the main UI thread.
- **And** the UI remains responsive while work is pending.

#### `AT-HIS-050` — Preserve MVVM and Clean Architecture boundaries

- **Level:** Static review + unit-test review
- **Requirements:** `NFR-HIS-ARC-01`, `NFR-HIS-ARC-02`,
  `NFR-HIS-TST-01`
- **Given** the History implementation and test suite are complete.
- **When** dependencies and imports are reviewed.
- **Then** History presentation depends on domain use cases/contracts and does
  not access Room DAOs or repository implementations directly.
- **And** shared validation does not require History code to call Scanner
  Fragment or Scanner ViewModel logic.
- **And** every Must requirement has at least one passing automated or manual
  acceptance test in the traceability matrix.

## 6. Manual end-to-end acceptance journey

This journey provides a junior developer with one repeatable emulator check. It
supplements, but does not replace, the focused scenarios above.

1. Install a clean Debug build and seed at least 31 Receipts and 61 Items.
2. Open History and verify Receipts, page size 15, the timestamp display, and
   `Page 1 of 3`.
3. Use Next, Previous, and the page Dropdown; verify page boundaries.
4. Change Receipt page size to 30, switch to All Items, set it to 50, then
   switch back and verify independent retained values.
5. Pull to refresh and verify the same mode/page size remains selected.
6. Open a Receipt, enter Edit, change its date, Chain, optional Branch, and one
   item; verify total recalculation.
7. Press Back, test Keep Editing, press Back again, and test `Don't Save`.
8. Edit again and save; verify the same Receipt ID is shown with updated values
   and its original time-of-day is preserved when only the date changed.
9. Delete a Receipt, cancel the first confirmation, then confirm it; verify no
   Undo appears and the page remains valid.
10. From Scanner, load or capture a receipt, verify date/time editing and save a
    unique Receipt.
11. Attempt `D-SAME`; verify the exact duplicate dialog. Select Discard and
    confirm the Review draft remains without an insertion.
12. Attempt it again, select Add, and verify exactly one new Receipt appears in
    History.
13. Repeat core browse/edit/delete operations with network access disabled.

## 7. Requirement traceability matrix

### 7.1 Functional requirements

| Requirement | Acceptance scenario(s) |
| --- | --- |
| `FR-HIS-BRW-01` | `AT-HIS-001` |
| `FR-HIS-BRW-02` | `AT-HIS-001` |
| `FR-HIS-BRW-03` | `AT-HIS-003` |
| `FR-HIS-BRW-04` | `AT-HIS-004` |
| `FR-HIS-BRW-05` | `AT-HIS-001` |
| `FR-HIS-BRW-06` | `AT-HIS-004` |
| `FR-HIS-BRW-07` | `AT-HIS-014` |
| `FR-HIS-BRW-08` | `AT-HIS-010`, `AT-HIS-011` |
| `FR-HIS-BRW-09` | `AT-HIS-001` |
| `FR-HIS-BRW-10` | `AT-HIS-001`, `AT-HIS-036`, `AT-HIS-037`, `AT-HIS-038` |
| `FR-HIS-PAG-01` | `AT-HIS-001` |
| `FR-HIS-PAG-02` | `AT-HIS-003` |
| `FR-HIS-PAG-03` | `AT-HIS-005` |
| `FR-HIS-PAG-04` | `AT-HIS-005` |
| `FR-HIS-PAG-05` | `AT-HIS-003` |
| `FR-HIS-PAG-06` | `AT-HIS-001`, `AT-HIS-004` |
| `FR-HIS-PAG-07` | `AT-HIS-006` |
| `FR-HIS-PAG-08` | `AT-HIS-006` |
| `FR-HIS-PAG-09` | `AT-HIS-007` |
| `FR-HIS-PAG-10` | `AT-HIS-007` |
| `FR-HIS-PAG-11` | `AT-HIS-008`, `AT-HIS-013` |
| `FR-HIS-PAG-12` | `AT-HIS-020` |
| `FR-HIS-PAG-13` | `AT-HIS-002`, `AT-HIS-004` |
| `FR-HIS-STA-01` | `AT-HIS-001`, `AT-HIS-002`, `AT-HIS-009` |
| `FR-HIS-STA-02` | `AT-HIS-001`, `AT-HIS-009` |
| `FR-HIS-STA-03` | `AT-HIS-001` |
| `FR-HIS-STA-04` | `AT-HIS-002` |
| `FR-HIS-STA-05` | `AT-HIS-009` |
| `FR-HIS-STA-06` | `AT-HIS-009` |
| `FR-HIS-STA-07` | `AT-HIS-011` |
| `FR-HIS-STA-08` | `AT-HIS-010`, `AT-HIS-012` |
| `FR-HIS-STA-09` | `AT-HIS-008` |
| `FR-HIS-STA-10` | `AT-HIS-008` |
| `FR-HIS-STA-11` | `AT-HIS-013` |
| `FR-HIS-STA-12` | `AT-HIS-012` |
| `FR-HIS-DEL-01` | `AT-HIS-015` |
| `FR-HIS-DEL-02` | `AT-HIS-015` |
| `FR-HIS-DEL-03` | `AT-HIS-015` |
| `FR-HIS-DEL-04` | `AT-HIS-016` |
| `FR-HIS-DEL-05` | `AT-HIS-016`, `AT-HIS-020` |
| `FR-HIS-DEL-06` | `AT-HIS-016`, `AT-HIS-018` |
| `FR-HIS-DEL-07` | `AT-HIS-016`, `AT-HIS-017` |
| `FR-HIS-DEL-08` | `AT-HIS-016`, `AT-HIS-018` |
| `FR-HIS-DEL-09` | `AT-HIS-019` |
| `FR-HIS-DEL-10` | `AT-HIS-021` |
| `FR-HIS-EDT-01` | `AT-HIS-014`, `AT-HIS-022` |
| `FR-HIS-EDT-02` | `AT-HIS-022` |
| `FR-HIS-EDT-03` | `AT-HIS-022`, `AT-HIS-028` |
| `FR-HIS-EDT-04` | `AT-HIS-022` |
| `FR-HIS-EDT-05` | `AT-HIS-025` |
| `FR-HIS-EDT-06` | `AT-HIS-023`, `AT-HIS-024` |
| `FR-HIS-EDT-07` | `AT-HIS-024` |
| `FR-HIS-EDT-08` | `AT-HIS-025` |
| `FR-HIS-EDT-09` | `AT-HIS-026` |
| `FR-HIS-EDT-10` | `AT-HIS-026` |
| `FR-HIS-EDT-11` | `AT-HIS-022` |
| `FR-HIS-EDT-12` | `AT-HIS-025` |
| `FR-HIS-EDT-13` | `AT-HIS-025` |
| `FR-HIS-EDT-14` | `AT-HIS-032` |
| `FR-HIS-EDT-15` | `AT-HIS-029` |
| `FR-HIS-EDT-16` | `AT-HIS-029`, `AT-HIS-031` |
| `FR-HIS-EDT-17` | `AT-HIS-030` |
| `FR-HIS-EDT-18` | `AT-HIS-029` |
| `FR-HIS-EDT-19` | `AT-HIS-031` |
| `FR-HIS-EDT-20` | `AT-HIS-033`, `AT-HIS-034`, `AT-HIS-035` |
| `FR-HIS-EDT-21` | `AT-HIS-034`, `AT-HIS-035` |
| `FR-HIS-EDT-22` | `AT-HIS-028` |
| `FR-HIS-EDT-23` | `AT-HIS-027` |
| `FR-HIS-EDT-24` | `AT-HIS-023` |
| `FR-HIS-DUP-01` | `AT-HIS-043` |
| `FR-HIS-DUP-02` | `AT-HIS-042`, `AT-HIS-043` |
| `FR-HIS-DUP-03` | `AT-HIS-039` |
| `FR-HIS-DUP-04` | `AT-HIS-040`, `AT-HIS-041` |
| `FR-HIS-DUP-05` | `AT-HIS-040` |
| `FR-HIS-DUP-06` | `AT-HIS-041` |
| `FR-HIS-DUP-07` | `AT-HIS-029` |
| `FR-HIS-TIM-01` | `AT-HIS-036` |
| `FR-HIS-TIM-02` | `AT-HIS-037` |
| `FR-HIS-TIM-03` | `AT-HIS-038` |
| `FR-HIS-TIM-04` | `AT-HIS-038` |
| `FR-HIS-STR-01` | `AT-HIS-030`, `AT-HIS-044` |
| `FR-HIS-STR-02` | `AT-HIS-044` |
| `FR-HIS-STR-03` | `AT-HIS-030`, `AT-HIS-044` |
| `FR-HIS-STR-04` | `AT-HIS-043` |

### 7.2 Non-functional requirements

| Requirement | Acceptance scenario(s) |
| --- | --- |
| `NFR-HIS-ARC-01` | `AT-HIS-050` |
| `NFR-HIS-ARC-02` | `AT-HIS-050` |
| `NFR-HIS-DAT-01` | `AT-HIS-049` |
| `NFR-HIS-DAT-02` | `AT-HIS-048` |
| `NFR-HIS-DAT-03` | `AT-HIS-026`, `AT-HIS-027`, `AT-HIS-048` |
| `NFR-HIS-PRV-01` | `AT-HIS-045` |
| `NFR-HIS-CMP-01` | `AT-HIS-047` |
| `NFR-HIS-USE-01` | `AT-HIS-047` |
| `NFR-HIS-REL-01` | `AT-HIS-046` |
| `NFR-HIS-TST-01` | `AT-HIS-050` and this traceability matrix |

## 8. Acceptance exit criteria

History Completion v1 is ready for acceptance only when all conditions below
are true:

1. Every Must functional and non-functional requirement has at least one test
   in section 7.
2. All automated unit, ViewModel, Room integration, and UI tests selected for
   the release pass on the tested commit.
3. All manual acceptance scenarios have recorded build/environment evidence.
4. No open defect causes data loss, duplicate unintended insertion, incorrect
   monetary calculation, invalid paging, main-thread database work, or repeated
   one-time effects.
5. No unresolved Critical or High defect remains within the approved v1 scope.
6. Existing-data migration/regression acceptance passes.
7. API 26 and current-target-API checks pass.
8. Product owner or Systems Analyst approves the exact duplicate dialog:
   `Possible duplicate receipt`, `Add anyway?`, `Discard`, and `Add`.
9. The implementation, tests, and these six History documents agree on the
   accepted behaviour.

## 9. Approval

| Role | Name | Decision | Date | Notes |
| --- | --- | --- | --- | --- |
| Systems Analyst | Victor Shih | Pending | — | — |
| Developer | — | Pending | — | — |
| Tester | — | Pending | — | — |
