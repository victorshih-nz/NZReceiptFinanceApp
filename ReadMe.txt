NZ Supermarket Receipt OCR & Expense Tracker
An Android-based expense management and localized financial analytics system built on Clean Architecture and MVVM patterns. The application integrates OCR text extraction with deterministic rule-based parsing to deliver structured data outputs from major New Zealand supermarket receipts (Woolworths, PAK'nSAVE, New World, Four Square).

🏗️ System Architecture
This project strictly adheres to Clean Architecture principles, decoupling core business logic from data frameworks and the UI layer to guarantee testability and smooth migration to cloud-sync interfaces.

Plaintext
app/src/main/java/com/example/nzreceiptapp/
├── domain/               # Core Business Logic Layer (Pure Java, zero Android dependencies)
│   ├── model/           # Enterprise Domain Models (e.g., ReceiptItem, ExpenseCategory)
│   └── usecase/         # Application Use Cases (e.g., ParseReceiptUseCase, GetFilteredExpensesUseCase)
│
├── data/                 # Data and Infrastructure Layer (Data Source Implementations)
│   ├── local/db/        # SQLite / Room DB configuration (Entities, DAOs)
│   ├── ocr/             # OCR text extraction abstraction wrappers
│   └── parser/          # Supermarket-specific parsers (WoolworthsParser, PaknSaveParser, etc.)
│
└── presentation/         # Presentation Layer (MVVM Architecture)
    ├── viewmodel/       # UI state preservation and Use Case invocation
    ├── view/            # Activities, Fragments, and custom charting (MPAndroidChart)
    └── adapter/         # UI list item adapters
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
