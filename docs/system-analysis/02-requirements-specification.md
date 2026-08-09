# 02 — Requirements Specification

## 1. Purpose and conventions

This specification describes externally meaningful behaviour. Implementation
class names are evidence, not the requirement itself. Status values refer to the
current `main` baseline.

Priority uses MoSCoW: Must, Should, Could, Won't for the current release.

## 2. Functional requirements

### 2.1 Image acquisition

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-IMG-01 | The system shall request camera permission before camera use | Must | Implemented | `ScannerFragment` |
| FR-IMG-02 | The user shall be able to capture a rear-camera receipt photo | Must | Implemented | CameraX `ImageCapture` |
| FR-IMG-03 | The user shall be able to select an image from Android content storage | Must | Implemented | `GetContent` launcher |
| FR-IMG-04 | The user shall be able to run a bundled Woolworths test receipt | Should | Implemented | Test Sample button |
| FR-IMG-05 | The system shall copy selected input into app-private persistent storage before processing | Must | Implemented | `LocalReceiptImageStore.persist` |
| FR-IMG-06 | The system shall report image-preparation failure without creating a draft | Must | Implemented | Scanner error state |

### 2.2 OCR and layout reconstruction

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-OCR-01 | The system shall extract Latin text from the stored image | Must | Implemented | `MLKitOCRService` |
| FR-OCR-02 | The system shall retain recognised line coordinates for row reconstruction | Must | Implemented | ML Kit bounding boxes |
| FR-OCR-03 | The system shall group vertically aligned fragments into receipt rows | Must | Implemented | `OcrTextLayoutBuilder` |
| FR-OCR-04 | The system shall order fragments left-to-right inside each reconstructed row | Must | Implemented | `OcrTextLayoutBuilder` |
| FR-OCR-05 | The system shall fall back to ML Kit's flattened text if no usable layout text is produced | Should | Implemented | `MLKitOCRService` |
| FR-OCR-06 | The system shall delete the private draft image when OCR fails | Must | Implemented | `ScannerViewModel` |

### 2.3 Chain detection and receipt parsing

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-PRS-01 | The user shall be able to select Auto detect, Woolworths, or PAK'nSAVE | Must | Implemented | Scanner spinner |
| FR-PRS-02 | Auto detect shall recognise Woolworths and Countdown as Woolworths | Must | Implemented | `ParserProvider.detectChain` |
| FR-PRS-03 | Auto detect shall recognise PAK'nSAVE despite apostrophe/space normalisation | Must | Implemented | `ParserProvider.detectChain` |
| FR-PRS-04 | A selected supported chain shall override auto-detection | Must | Implemented | `ParseReceiptUseCase` |
| FR-PRS-05 | Unsupported or undetected chains shall produce a user-visible parsing error | Must | Implemented | `ParseReceiptUseCase` |
| FR-PRS-06 | The Woolworths parser shall return structured items from supported Woolworths/Countdown rows | Must | Implemented | `WoolworthsParser` |
| FR-PRS-07 | The PAK'nSAVE parser shall return structured items from supported PAK'nSAVE rows | Must | Implemented | `PakNSaveParser` |
| FR-PRS-08 | A parser shall return receipt-level printed total when recognised | Should | Implemented | `ParsedReceipt` |
| FR-PRS-09 | A parse producing no items shall be rejected | Must | Implemented | `ParseReceiptUseCase` |
| FR-PRS-10 | The system shall preserve raw OCR text in the receipt draft | Must | Implemented | `Receipt.rawOcrText` |
| FR-PRS-11 | The system should recognise the printed purchase date | Should | Planned | Current time is used |
| FR-PRS-12 | The system should expose parser warnings separately from fatal errors | Should | Planned | Current single error message |

### 2.4 Category classification

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-CAT-01 | The system shall load bundled category rules before first classification | Must | Implemented | `BundledCategoryInitializer` |
| FR-CAT-02 | Category initialisation shall be safe to repeat without duplicating logical categories | Must | Implemented | Find/reuse and rule replace |
| FR-CAT-03 | Item-name matching shall be case-insensitive and ignore common units/symbols | Must | Implemented | `CategoryClassifier` |
| FR-CAT-04 | When multiple rules match, the longest keyword shall win | Must | Implemented | `CategoryClassifier` |
| FR-CAT-05 | An unmatched item may remain uncategorised | Must | Implemented | Null category |
| FR-CAT-06 | The user shall be able to replace an automatically assigned category during review | Must | Implemented | Review category spinner |
| FR-CAT-07 | An authorised user should be able to manage category rules | Could | Planned | No management UI |

### 2.5 Draft review and validation

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-REV-01 | OCR/parsing shall create an in-memory draft and shall not immediately save it | Must | Implemented | `ScannerUiState.READY_FOR_REVIEW` |
| FR-REV-02 | The user shall be able to edit chain and branch | Must | Implemented | Review fields |
| FR-REV-03 | The user shall be able to edit item name, quantity, unit, price, and category | Must | Implemented | `ReceiptReviewAdapter` |
| FR-REV-04 | The user shall be able to add and remove items | Must | Implemented | Review adapter actions |
| FR-REV-05 | The review screen shall display a calculated total | Must | Implemented | `refreshCalculatedTotal` |
| FR-REV-06 | The review screen shall display a recognised printed total or indicate it was not recognised | Must | Implemented | Review labels |
| FR-REV-07 | A difference greater than one cent shall display a total mismatch warning | Should | Implemented | Review comparison |
| FR-REV-08 | Save shall require non-empty chain, branch, and at least one valid item | Must | Implemented | ViewModel/adapter validation |
| FR-REV-09 | Cancelling review shall discard the draft and delete its private image | Must | Implemented | `discardDraft` |
| FR-REV-10 | A total mismatch should optionally block save under a configurable policy | Could | Planned | Warning only |

### 2.6 Persistence and deletion

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-DAT-01 | Saving shall persist store, receipt, items, and discounts in one Room transaction | Must | Implemented | `ReceiptDao.saveFullReceipt` |
| FR-DAT-02 | The system shall reuse an existing store with the same chain/branch | Must | Implemented | Unique index and DAO lookup |
| FR-DAT-03 | Saved receipt data shall include raw OCR, private image URI, and optional printed total | Must | Implemented | Room schema v2 |
| FR-DAT-04 | Item prices and discounts shall be stored as integer cents | Must | Implemented | Domain and Room fields |
| FR-DAT-05 | Deleting a receipt shall delete dependent items and discounts | Must | Implemented | Foreign-key cascades |
| FR-DAT-06 | Deleting a receipt shall delete its private stored image | Must | Implemented | `ReceiptRepositoryImpl` |
| FR-DAT-07 | A store with no remaining receipts shall be removed | Should | Implemented | DAO transaction |
| FR-DAT-08 | Database upgrades shall preserve supported existing data through explicit migrations | Must | Partial | Migration 1→2 exists; no migration test |

### 2.7 History, detail, and analytics

| ID | Requirement | Priority | Status | Evidence |
| --- | --- | --- | --- | --- |
| FR-HIS-01 | The user shall be able to browse receipts newest-first, ten per page | Must | Implemented | History receipt mode |
| FR-HIS-02 | The user shall be able to browse all items newest-first, 25 per page | Should | Implemented | All Items mode |
| FR-HIS-03 | The user shall be able to refresh the current history view | Should | Implemented | Swipe refresh |
| FR-HIS-04 | The user shall be able to navigate between history pages | Must | Implemented | Previous/Next |
| FR-HIS-05 | The system should disable Next after the final page | Should | Planned | Current unbounded next page |
| FR-HIS-06 | The user shall be able to open a saved receipt detail | Must | Implemented | Detail navigation/ViewModel |
| FR-HIS-07 | The user shall be able to delete a saved receipt from History | Must | Implemented | Delete use case |
| FR-ANL-01 | The system shall be able to aggregate final item spending by category | Should | Implemented | `GetAnalyticsUseCase` |
| FR-ANL-02 | The user should be able to view category spending in Analytics | Should | Planned | Empty Analytics fragment |

## 3. Non-functional requirements

| ID | Requirement | Status / evidence |
| --- | --- | --- |
| NFR-ARC-01 | Domain code shall remain independent of Android/framework implementations | Implemented by package dependency rule |
| NFR-ARC-02 | Presentation shall access persistence only through ViewModel/use case/contracts | Implemented for current screens |
| NFR-MNT-01 | Each major responsibility shall have one clear owner component | Mostly implemented; component catalogue records ownership |
| NFR-MNT-02 | New supermarket formats shall implement `IReceiptParser` and register through the factory | Implemented extension point |
| NFR-TST-01 | Domain, parser, repository, and ViewModel rules shall be JVM-testable | Implemented; 31 tests |
| NFR-TST-02 | Every database migration shall have an automated migration test | Planned gap |
| NFR-REL-01 | Long-running image/OCR/database work shall not block the main UI thread | Implemented through callbacks and injected executor |
| NFR-REL-02 | Draft images shall be cleaned up after OCR/parse failure or user cancellation | Implemented |
| NFR-REL-03 | User-confirmed data shall not be silently replaced by OCR output | Implemented through explicit save |
| NFR-DAT-01 | Monetary storage shall avoid floating-point currency fields | Implemented with cents; quantity remains decimal |
| NFR-DAT-02 | Room schema history shall be committed to version control | Implemented for v2 |
| NFR-PRV-01 | The app shall not require an account or app backend for core use | Implemented |
| NFR-PRV-02 | Receipt images shall be stored in app-private storage | Implemented |
| NFR-CMP-01 | The app shall support Android API 26 through target API 36 | Configured; device matrix not automated |
| NFR-USA-01 | The user shall be shown progress and actionable failure text during scanning | Partial; phases/errors exist, wording is basic |
| NFR-USA-02 | Core controls shall satisfy Android accessibility and touch-target guidance | Not formally verified |
| NFR-OPS-01 | Pull requests should run unit tests and debug assembly automatically | Planned; no GitHub Actions |

## 4. Requirement acceptance rule

A requirement may be marked Implemented only when:

1. production code exists on `main`;
2. the behaviour is reachable through the intended system path;
3. automated or manual acceptance evidence exists;
4. known exceptions are documented;
5. related requirements, rules, and diagrams are consistent.

Code existence alone is insufficient. For example, `GetAnalyticsUseCase` makes
category aggregation implemented, but the user-visible Analytics outcome remains
Planned because the screen is empty.
