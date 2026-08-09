# 08 — Java Component Catalogue

This catalogue assigns one primary responsibility to every production Java file
in the current project. “Used by” identifies the main runtime consumer, not every
test or incidental reference.

## 1. Application entry and composition

| Component | Responsibility | Mainly used by |
| --- | --- | --- |
| `MainActivity` | Inflate the activity shell; connect NavHost to bottom navigation | Android launcher |
| `NzReceiptApplication` | Create and retain the application-level `AppContainer` | Android application lifecycle |
| `di/AppContainer` | Composition root for database, services, repositories, use cases, image store, and executor | Application and factory |
| `di/ViewModelFactory` | Construct supported ViewModels from container dependencies | Fragments |

## 2. Domain models and rules

| Component | Responsibility | Mainly used by |
| --- | --- | --- |
| `domain/model/Store` | Domain identity for supermarket chain and branch | Receipt/use case/repository |
| `domain/model/Receipt` | Receipt aggregate and total calculations | Use cases, ViewModels, repository |
| `domain/model/ReceiptItem` | Item values, subtotal/discount calculations, category copy | Parsers, review, repository |
| `domain/model/ItemDiscount` | Item discount value object and discount type | ReceiptItem and repository |
| `domain/model/Category` | Parent/child category domain model | Classifier, review, repository |
| `domain/model/ParsedReceipt` | Format-parser result: items plus optional printed total | Parsers and parse use case |
| `domain/model/ReceiptItemSummary` | Item plus store/date context for All Items history | History repository/UI |
| `domain/model/CategorySpending` | Category and aggregated amount result | Analytics use case |
| `domain/logic/CategoryClassifier` | Normalise item names and choose longest matching keyword | Parse use case |

## 3. Domain contracts

| Component | Responsibility | Current implementation |
| --- | --- | --- |
| `domain/parser/IReceiptParser` | Standard chain-specific parse operation | Woolworths/PAK'nSAVE parsers |
| `domain/parser/IParserFactory` | Detect chain and obtain parser | `ParserProvider` |
| `domain/repository/IReceiptRepository` | Save/query/page/delete receipt aggregates | `ReceiptRepositoryImpl` |
| `domain/repository/ICategoryRepository` | Query/save category hierarchy and rules | `CategoryRepositoryImpl` |
| `domain/service/IOCRService` | Async image-to-text boundary and callback | `MLKitOCRService` |
| `domain/service/IReceiptImageStore` | Persist/delete owned receipt image | `LocalReceiptImageStore` |
| `domain/service/ICategoryInitializer` | Ensure classification data exists | `BundledCategoryInitializer` |

## 4. Domain use cases

| Component | Responsibility | Called by |
| --- | --- | --- |
| `ParseReceiptUseCase` | Initialise categories, resolve parser, parse, classify, and build draft | Scanner ViewModel |
| `SaveReceiptUseCase` | Validate non-null receipt and delegate save | Scanner ViewModel |
| `DeleteReceiptUseCase` | Delegate receipt deletion | History ViewModel |
| `GetReceiptByIdUseCase` | Retrieve one receipt aggregate | Receipt Detail ViewModel |
| `GetReceiptsPagedUseCase` | Convert page/page-size to repository limit/offset | History ViewModel |
| `GetAllItemsPagedUseCase` | Retrieve contextual item summaries by page | History ViewModel |
| `GetCategoriesUseCase` | Retrieve available categories | Scanner ViewModel |
| `GetAnalyticsUseCase` | Aggregate item final subtotals by category | Tests; future Analytics ViewModel |

## 5. Data — local infrastructure

| Component | Responsibility | Mainly used by |
| --- | --- | --- |
| `data/local/AppDatabase` | Room database v2 singleton, entity list, and migration 1→2 | AppContainer |
| `data/local/Converters` | Convert `LocalDateTime` and other non-SQLite values for Room | Room |
| `data/local/BundledCategoryInitializer` | Read TSV and seed/reuse category hierarchy and rules | Parse use case through contract |
| `data/local/LocalReceiptImageStore` | Copy URIs to private JPEGs and safely delete owned files | Scanner ViewModel/repository |
| `data/local/dao/ReceiptDao` | Receipt queries plus transactional aggregate save/delete | Receipt repository |
| `data/local/dao/CategoryDao` | Category/rule inserts and queries | Category repository |

## 6. Data — Room entities and relation projections

| Component | Room role | Meaning |
| --- | --- | --- |
| `CategoryEntity` | Entity | `categories` row with optional parent FK |
| `CategoryRuleEntity` | Entity | Unique keyword mapped to category FK |
| `StoreEntity` | Entity | Unique chain/branch store row |
| `ReceiptEntity` | Entity | Receipt header/source metadata row |
| `ReceiptItemEntity` | Entity | Parsed/reviewed item row |
| `ItemDiscountEntity` | Entity | Discount owned by one item |
| `ReceiptItemWithDiscounts` | Relation projection | One item with discounts and optional category |
| `ReceiptWithItems` | Relation projection | Receipt, Store, and related item aggregates |
| `ReceiptItemRow` | Query projection | Item plus store/date context for All Items history |

Room entities are data-layer records. They must be mapped to domain models before
leaving repository implementations.

## 7. Data — OCR and parsing

| Component | Responsibility | Mainly used by |
| --- | --- | --- |
| `data/ocr/MLKitOCRService` | Create ML Kit recogniser, load image, collect line boxes, return reconstructed text | Scanner ViewModel through `IOCRService` |
| `data/ocr/OcrTextLayoutBuilder` | Pure-Java grouping/sorting of positioned fragments into rows | ML Kit service and JVM tests |
| `data/parser/ParserProvider` | Normalise chain text, auto-detect brand, return reusable parser instance | Parse use case through factory contract |
| `data/parser/WoolworthsParser` | Parse supported Woolworths/Countdown row patterns and total | ParserProvider |
| `data/parser/PakNSaveParser` | Parse supported PAK'nSAVE row patterns and total | ParserProvider |

Concrete parsers own receipt-format interpretation only. Shared category
classification belongs to `ParseReceiptUseCase`/`CategoryClassifier`.

## 8. Data — repository implementations

| Component | Responsibility | Mainly used by |
| --- | --- | --- |
| `data/repository/ReceiptRepositoryImpl` | Map receipt aggregates between domain/Room, page queries, coordinate delete/image cleanup | Receipt use cases |
| `data/repository/CategoryRepositoryImpl` | Map category entities/rules to domain models and save seed data | Classifier/category use case |

## 9. Presentation — state and ViewModels

| Component | Responsibility | Screen owner |
| --- | --- | --- |
| `presentation/base/BaseViewModel` | Shared loading/error LiveData for legacy screen ViewModels | History and Receipt Detail |
| `presentation/viewmodel/ScannerUiState` | Immutable Scanner phase, draft, categories, and error state | Scanner/Review |
| `presentation/viewmodel/ScannerViewModel` | Coordinate private image, OCR, parse, review validation, explicit save, and discard | Activity-scoped Scanner/Review |
| `presentation/viewmodel/HistoryViewModel` | Manage receipt/all-item mode, page, load, refresh, and delete | History Fragment |
| `presentation/viewmodel/ReceiptDetailViewModel` | Load one receipt and publish loading/error/result | Receipt Detail Fragment |

There is currently no `AnalyticsViewModel`.

## 10. Presentation — Fragments

| Component | Responsibility | Layout/navigation |
| --- | --- | --- |
| `presentation/view/ScannerFragment` | Camera permission/preview/capture, gallery/sample actions, Scanner state rendering | `fragment_scanner.xml` |
| `presentation/view/ReceiptReviewFragment` | Bind shared draft, edit/validate, total comparison, save/cancel navigation | `fragment_receipt_review.xml` |
| `presentation/view/HistoryFragment` | Tabs, adapters, pagination, refresh, detail/delete actions | `fragment_history.xml` |
| `presentation/view/ReceiptDetailFragment` | Render receipt loaded by ID | `fragment_receipt_detail.xml` |
| `presentation/view/AnalyticsFragment` | Placeholder only; currently returns an empty View | Analytics destination |

Fragment responsibilities should remain limited to Android UI, lifecycle,
navigation, and rendering. Business decisions belong in domain/use cases.

## 11. Presentation — adapters

| Component | Responsibility | Used by |
| --- | --- | --- |
| `presentation/adapter/ReceiptAdapter` | Render receipt history rows and forward open/delete actions | History receipt mode |
| `presentation/adapter/ReceiptItemSummaryAdapter` | Render item history with store/date context | History All Items mode |
| `presentation/adapter/ReceiptItemAdapter` | Render items within saved receipt detail | Receipt Detail |
| `presentation/adapter/ReceiptReviewAdapter` | Maintain editable item rows, category choices, add/remove, and build validated domain items | Review Fragment |

## 12. Generated classes (not source files)

The project also uses generated code that must not be manually edited:

- View Binding classes such as `FragmentScannerBinding`;
- Room database implementation and DAO implementations;
- Android resource identifier class `R`;
- BuildConfig and APK/resource intermediates.

Generated files are recreated by Gradle from XML, annotations, and build
configuration. Source changes belong in the input XML/Java/Gradle files.

## 13. Finding a component from a user action

| User action | Start tracing here | Then follow |
| --- | --- | --- |
| Tap Test Sample | `ScannerFragment.loadSampleFromAssets` | `processImage` → Scanner VM |
| OCR finishes | `MLKitOCRService` success callback | Scanner VM `parseDraft` |
| Parser selected | `ParserProvider.getParser` | Concrete parser `parse` |
| Category assigned | `ParseReceiptUseCase.classifyItems` | `CategoryClassifier.classify` |
| Save review | `ReceiptReviewFragment.saveReceipt` | Scanner VM → Save use case → repository |
| Open History | `HistoryFragment.onViewCreated` | History VM → paged use case → repository/DAO |
| Delete receipt | ReceiptAdapter callback | History VM → delete use case → repository/DAO/image store |

This action-first tracing method is usually more effective than opening classes
alphabetically.
