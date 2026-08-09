# 06 — Verification and Traceability

## 1. Test strategy

The current project uses three verification levels:

| Level | Purpose | Current mechanism |
| --- | --- | --- |
| Local JVM unit | Business rules and orchestration without emulator | JUnit 4, Mockito, Arch Core Testing |
| Build verification | Java/XML/resources/View Binding/Room annotation processing | `assembleDebug` |
| Manual acceptance | Camera, ML Kit, navigation, UI, and end-to-end persistence | Emulator or Android device |

There are currently 31 local JVM tests. There are no GitHub Actions gates,
instrumentation flows, screenshot tests, or automated Room migration tests.

## 2. Automated test groups

| Test ID | Test class | Main coverage |
| --- | --- | --- |
| T-OCR-01 | `OcrTextLayoutBuilderTest` | Row joining, order, filtering, parser compatibility |
| T-PARSER-01 | `ParserProviderTest` | Supported parser lookup and canonical detection |
| T-PARSER-02 | `WoolworthsParserTest` | Standard, weighted, multi-buy, total, garbage receipts |
| T-PARSER-03 | `PakNSaveParserTest` | PAK'nSAVE item and total parsing |
| T-CAT-01 | `CategoryClassifierTest` | Longest match, word boundaries, normalisation, no match |
| T-UC-01 | `ParseReceiptUseCaseTest` | Initialisation, parsing, classification, auto-detect error |
| T-UC-02 | `SaveReceiptUseCaseTest` | Save/non-save behaviour |
| T-UC-03 | `DeleteReceiptUseCaseTest` | Repository delete delegation |
| T-UC-04 | `GetAnalyticsUseCaseTest` | Spending aggregation by category |
| T-REPO-01 | `ReceiptRepositoryImplTest` | Mapping and image deletion behaviour |
| T-VM-01 | `ScannerViewModelTest` | OCR success/failure, review-before-save, explicit save |
| T-VM-02 | `HistoryViewModelTest` | First-page load and reload after delete |
| T-VM-03 | `ReceiptDetailViewModelTest` | Receipt result and missing receipt error |

## 3. Requirements traceability matrix

| Requirement(s) | Use case | Design/component | Verification | Gap |
| --- | --- | --- | --- | --- |
| FR-IMG-01..04 | UC-01 | `ScannerFragment`, CameraX, assets | AT-SCAN-01, AT-SCAN-02 | No instrumentation test |
| FR-IMG-05..06 | UC-01 | `LocalReceiptImageStore`, Scanner VM | T-REPO-01, AT-ERR-01 | Direct image-store test absent |
| FR-OCR-01 | UC-01 | `MLKitOCRService` | AT-SCAN-01/02 | ML Kit not unit tested |
| FR-OCR-02..05 | UC-01 | `OcrTextLayoutBuilder` | T-OCR-01 | Covered |
| FR-OCR-06 | UC-01 | Scanner VM, image store | T-VM-01 | Verify deletion mock in future |
| FR-PRS-01..05 | UC-01 | Spinner, Provider, Parse use case | T-PARSER-01, T-UC-01, AT-SCAN-01 | Covered except UI automation |
| FR-PRS-06 | UC-01 | `WoolworthsParser` | T-PARSER-02 | More real fixtures required |
| FR-PRS-07 | UC-01 | `PakNSaveParser` | T-PARSER-03 | More real fixtures required |
| FR-PRS-08..10 | UC-01/02 | `ParsedReceipt`, Receipt | T-PARSER-02/03, T-UC-01 | Covered |
| FR-PRS-11..12 | UC-01 | Planned parser result | None | Planned |
| FR-CAT-01..06 | UC-06/02 | Initializer, classifier, review | T-CAT-01, T-UC-01, AT-REV-01 | Initializer integration not automated |
| FR-REV-01 | UC-01/02 | Scanner state/ViewModel | T-VM-01 | Covered |
| FR-REV-02..08 | UC-02 | Review Fragment/Adapter/VM | AT-REV-01, AT-REV-02 | Adapter validation not unit tested |
| FR-REV-09 | UC-03 | Scanner VM/ImageStore | AT-REV-03 | Add explicit unit verification |
| FR-DAT-01..04 | UC-02 | Repository, DAO, Room v2 | T-REPO-01, AT-DAT-01 | Room integration test absent |
| FR-DAT-05..07 | UC-05 | FKs, DAO, repository | T-UC-03, T-REPO-01, AT-DAT-02 | Cascade/integration not automated |
| FR-DAT-08 | Migration | `MIGRATION_1_2`, schema JSON | Build only | Migration test required |
| FR-HIS-01..04 | UC-04 | History VM/Fragment/DAO | T-VM-02, AT-HIS-01 | Final-page behaviour missing |
| FR-HIS-06..07 | UC-05 | Detail VM, delete use case | T-VM-03, T-UC-03, AT-HIS-02 | UI automation absent |
| FR-ANL-01 | UC-07 | Analytics use case | T-UC-04 | Covered |
| FR-ANL-02 | UC-07 | Planned UI/ViewModel | None | Planned |

## 4. Manual acceptance tests

### AT-SCAN-01 — Bundled sample to saved receipt

**Covers:** FR-IMG-04, FR-OCR-01, FR-PRS-02/06/08, FR-CAT-01..06,
FR-REV-01..08, FR-DAT-01..04.

1. Install a clean debug build.
2. Open Scanner with Auto detect.
3. Tap Test Sample.
4. Confirm phases reach `READY_FOR_REVIEW` without error.
5. Open Review and confirm Woolworths, items, categories, raw OCR, and totals.
6. Edit one item and category.
7. Save.
8. Confirm History and Receipt Detail reflect edits.

**Pass:** Exactly one confirmed receipt appears with the edited values.

### AT-SCAN-02 — Camera and Gallery acquisition

1. Deny then grant camera permission; confirm expected message/recovery.
2. Capture a clear supported receipt.
3. Repeat with Gallery.
4. Test Auto detect and explicit chain selection.

**Pass:** Both inputs reach Review, and explicit chain selection is honoured.

### AT-REV-01 — Review item editing

1. Produce a draft.
2. Edit chain, branch, item name, quantity, unit, price, and category.
3. Add one item and remove another.
4. Observe calculated-total changes.
5. Save and reopen from History.

**Pass:** Persistent data matches reviewed data, not original OCR output.

### AT-REV-02 — Validation and total warning

1. Clear branch and attempt save.
2. Enter invalid numeric item input and attempt save.
3. Create a printed/calculated mismatch over one cent.

**Pass:** Invalid data is blocked with a message; total mismatch is visible but
does not block save under current `BR-TOTAL-08`.

### AT-REV-03 — Cancel draft

1. Produce a draft.
2. Cancel Review.
3. Open History.

**Pass:** No new receipt exists. Debug inspection should show no retained private
draft image.

### AT-HIS-01 — History modes and pagination

1. Create more than ten receipts or seed representative data.
2. Verify newest-first Receipts pages.
3. Switch to All Items and verify page size/ordering.
4. Pull to refresh and navigate Previous/Next.

**Pass:** Lists and page number reflect the selected mode. Record the known
empty-final-page limitation separately.

### AT-HIS-02 — Detail and deletion

1. Open a receipt from History.
2. Compare displayed values with Review/saved values.
3. Delete the receipt.
4. Refresh History.

**Pass:** Receipt disappears, dependent rows and private image are removed, and
a Store used by other receipts remains.

### AT-ERR-01 — Failure handling

Test unreadable image, unsupported branding with Auto detect, and malformed
receipt text.

**Pass:** The app reports a meaningful error, does not save a receipt, and
returns to a usable Scanner state.

### AT-DAT-01 — Upgrade from database v1 to v2

This test is currently manual and should become an automated Room migration
test.

1. Install/build a v1 database with representative and duplicate Store data.
2. Upgrade without uninstalling or clearing app data.
3. Open saved receipts.
4. Verify source fields are nullable, Store references are canonical, indexes
   exist, and SQLite integrity check passes.

### AT-DAT-02 — Delete ownership rules

Create two receipts for one Store, delete the first, then delete the second.

**Pass:** Store remains after the first deletion and is removed after the last;
each receipt image follows its receipt lifecycle.

## 5. Build verification

Windows:

```powershell
.\gradlew.bat clean testDebugUnitTest
.\gradlew.bat assembleDebug
```

Expected results:

- all JVM tests pass;
- no Java/XML/resource/View Binding compilation failure;
- Room schema export completes;
- debug APK exists under `app/build/outputs/apk/debug`.

## 6. Release gate recommendation

Before a future tagged release:

- all Must requirements have acceptance evidence;
- parser fixture regression suite passes for every supported chain;
- `testDebugUnitTest` and `assembleDebug` pass in CI;
- Room migrations pass automated tests from every supported schema version;
- camera/gallery/manual flows pass on at least one physical device and one
  emulator;
- accessibility checks have no critical issue;
- no unresolved data-loss, privacy, or crash-severity risk remains;
- documentation and Room schema match the release commit.
