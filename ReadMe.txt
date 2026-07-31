NZ Supermarket Receipt OCR & Expense Tracker
An Android-based expense management and localized financial analytics system built on Clean Architecture and MVVM patterns. The application integrates OCR text extraction with deterministic rule-based parsing to deliver structured data outputs from major New Zealand supermarket receipts (Woolworths, PAK'nSAVE, New World, Four Square).

🏗️ System Architecture
This project strictly adheres to Clean Architecture principles, decoupling core business logic from data frameworks and the UI layer to guarantee testability and smooth migration to cloud-sync interfaces.
app/src/main/java/com/example/nzreceiptapp/
├── domain/                         # 核心業務邏輯層 (純 Java/Kotlin，無 Android 依賴)
│   ├── logic/                      # 業務邏輯實作
│   │   └── CategoryClassifier      # 自動分類識別器
│   ├── model/                      # 領域實體模型 (Entities)
│   │   ├── Receipt / ReceiptItem   # 收據、品項模型
│   │   ├── Category / Store        # 分類、商店模型
│   │   └── ReceiptItemSummary      # 用於扁平化清單的彙整模型
│   ├── parser/                     # 解析器介面
│   │   ├── IReceiptParser          # 超市解析器標準介面
│   │   └── IParserFactory          # 解析器工廠介面
│   ├── repository/                 # 資料倉儲介面
│   │   ├── IReceiptRepository      # 收據儲存與讀取介面
│   │   └── ICategoryRepository     # 分類規則讀取介面
│   ├── service/                    # 外部服務介面
│   │   └── IOCRService             # 文字辨識服務介面
│   └── usecase/                    # 應用案例 (Use Cases)
│       ├── ParseReceiptUseCase     # 執行收據解析
│       ├── SaveReceiptUseCase      # 執行收據儲存
│       ├── GetAnalyticsUseCase     # 獲取統計分析
│       └── GetReceiptsPagedUseCase # 分頁獲取歷史紀錄
│
├── data/                           # 資料與基礎設施層 (實作 Domain 介面)
│   ├── local/                      # 本地資料庫 (Room/SQLite)
│   │   ├── dao/                    # Data Access Objects (SQL 邏輯)
│   │   ├── entity/                 # 資料表實體與關係類 (Relations)
│   │   └── AppDatabase             # Room 資料庫配置
│   ├── ocr/                        # OCR 具體實作
│   │   └── MLKitOCRService         # Google ML Kit 文字辨識實作
│   ├── parser/                     # 特定超市解析邏輯
│   │   ├── WoolworthsParser        # Woolworths 格式解析 (Regex)
│   │   ├── PakNSaveParser          # PAK'nSAVE 格式解析
│   │   └── ParserProvider          # 解析器分發實作
│   └── repository/                 # 資料儲存實作
│       ├── ReceiptRepositoryImpl    # 處理資料映射 (Mapping) 與資料庫操作
│       └── CategoryRepositoryImpl   # 處理分類規則讀取
│
└── presentation/                   # 展示層 (UI 與 MVVM)
    ├── base/                       # 基礎類別 (BaseViewModel, Factory)
    ├── viewmodel/                  # 介面狀態管理 (UI State)
    │   ├── ScannerViewModel        # 掃描流程狀態
    │   └── HistoryViewModel        # 歷史清單與分頁狀態
    ├── view/                       # Activity / Fragments (UI 介面)
    │   ├── ScannerFragment         # 相機與拍照畫面
    │   ├── HistoryFragment         # 歷史分頁清單
    │   └── ReceiptDetailFragment   # 收據明細詳情頁
    └── adapter/                    # RecyclerView 適配器
        ├── ReceiptAdapter          # 收據卡片適配器
        └── ReceiptItemAdapter      # 商品明細適配器


🛠️ Tech Stack
Development Language: Java 26

Minimum SDK Support: Android SDK API 36.1 (Android 16.0)

Database Persistence: SQL (SQLite managed via Room DB)

Unit Testing Framework: JUnit 4 / Mockito

Data Visualisation: MPAndroidChart

⚙️ Core Parser Specifications
The core parsing engine (ReceiptParser) implements a two-tier regular expression pipeline combined with a pointer retrospection mechanism to safely handle unstructured text extraction over erratic OCR outputs:

Noise Reduction: Systematically matches and drops non-item text blocks including store addresses, metadata headers, tax invoices summaries, payment types, and EFTPOS/cash change lines.

GST Sanitisation: Employs trailing pattern matches (?:\s+[A-Z*])?\s*$ to strip regional tax codes (G, N, or *) from the terminal ends of transaction rows, preventing data type parsing exceptions during float conversions.

Weighted Item Extraction: Targets and extracts multi-variable line entries containing item units, weight measurements, and unit pricing variables (e.g., parsing BANANAS LOOSE 0.645 kg @ $3.49 /kg).

Multiline Multi-Buy Fallback: Resolves dropped text rows caused by nested multi-buy discount blocks (e.g., 2 @ 1.50 appearing on the subsequent line). When identified, the parser initiates a retro-step pointer evaluation to update the quantity and unitPriceCents arrays belonging to the previously pushed parent entity.

⚡ Getting Started & Testing
Environment Setup
Clone the repository:

Bash
git clone https://github.com/victorshih-nz/NZRecriptFinanceApp.git
Open the project directory within Android Studio and let the Gradle dependencies sync completely.

Executing Local Unit Tests
The test suite covers exhaustive receipt variants, focusing on decimal weight parsers, multi-line fallbacks, whitespace anomalies, and GST tag sanitisation.

Bash
./gradlew testDebugUnitTest
