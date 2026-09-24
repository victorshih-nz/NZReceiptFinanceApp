# NZ Receipt Finance App

An Android learning project for capturing New Zealand supermarket receipts, reviewing OCR results, saving purchases locally, and exploring spending by date and category. Built in **Java** with **MVVM**, Clean Architecture package boundaries, and **Room**.

> **Current scope:** local-only prototype. No account registration, cloud backup, server-side data collection, price alerts, or production release is implemented yet. OCR and receipt parsing require review; do not rely on extracted totals without checking them.

## Features

| Area | Current implementation |
| --- | --- |
| Receipt input | CameraX capture, gallery selection, and bundled Woolworths test sample |
| OCR | Google ML Kit text recognition; layout reconstruction of text fragments |
| Parsing | Auto-detection and rule-based parsers for Woolworths/Countdown and PAK'nSAVE |
| Review | Edit store and item details, quantities, prices, and categories before saving |
| Local history | Browse receipts and individual items, inspect receipt detail, delete records; refresh on return from saving |
| Analytics | Annual, monthly, and daily spending; tap to select a period, double-tap year/month to drill down; horizontally scroll daily/yearly charts; category totals and percentages |
| Storage | Room SQLite database and app-private receipt images; no remote synchronization |

### Analytics calculation rules

- **Total spending** is the sum of final payable amounts of receipts in the selected period.
- **Category spending** is the sum of final item subtotals, grouped by category; unclassified items appear under **Uncategorized**.
- Category percentages use the **total item subtotals** as the denominator. Receipt-level discounts affect payable spending but are not allocated across categories, so the category total and payable total can differ.
- All amounts are calculated in integer NZD cents. The period follows the saved purchase date, with an inclusive start and exclusive end; undated receipts are omitted from dated analytics. Empty calendar periods appear as zero.

See [Analytics documentation](docs/ANALYTICS.md) for usage and verification details.

## Run the app

**Requirements:** Android Studio, Android SDK for the configured compile API (Android 36, minor API 1), a compatible JDK (Android Studio bundled JDK is recommended), and an Android emulator/device running **API 26 or higher**. Internet is needed for initial dependency download and may be required for ML Kit model delivery.

1. Clone the repository and open its root in Android Studio:

   ```bash
   git clone https://github.com/victorshih-nz/NZReceiptFinanceApp.git
   cd NZReceiptFinanceApp
   ```

2. Allow Gradle Sync to complete and check the `local.properties` Android SDK path if Android Studio cannot find the SDK. Do not commit `local.properties`.
3. Select the **app** run configuration, choose an emulator/device, and click **Run**. Allow camera access when prompted if using capture.
4. Go to **Scanner**. Select automatic detection or a supported supermarket, take a photo, choose an existing image, or use **Test Sample**. Review and correct the receipt, then save.
5. Open **History** to inspect saved receipts/items. Open **Analytics** to inspect totals; use **Years**, month bars and double-tap drill-down to navigate periods.

### Build and tests (Windows PowerShell)

From the project root:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```

The debug APK is generated under `app/build/outputs/apk/debug/`. JVM test reports are under `app/build/reports/tests/testDebugUnitTest/`.

The repository also contains Room instrumented tests; run these **on a dedicated disposable emulator**, not the emulator holding personal receipts:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.nzreceiptapp.data.local.dao.AnalyticsReceiptDaoTest" --console=plain
```

These tests create an in-memory database, but the effect of test installation/execution on a development emulator's existing App data has not yet been fully isolated and verified. Keep a backup of important receipts. Avoid clearing app storage, uninstalling the app, or resetting the emulator if you want to keep its local data.

On macOS/Linux, replace `gradlew.bat` with `./gradlew`.

## Architecture

```text
app/src/main/java/com/example/nzreceiptapp/
├── presentation/    # Fragments, chart view, ViewModels, UI state
├── domain/          # Receipt/category models, contracts and use cases
├── data/            # Room, repositories, OCR, parsers and image storage
└── di/              # AppContainer and ViewModelFactory
```

The UI observes ViewModel state. ViewModels call use cases; domain code depends on interfaces rather than Android persistence or OCR implementations. The data layer supplies the concrete Room, parser, and ML Kit implementations. Background work runs through injected executors to support unit testing. Room schema is versioned and stored under `app/schemas/`.

**Core stack:** Java 11 source compatibility; Android Views/XML/View Binding; Navigation; CameraX; ML Kit; Room 2.6.1; LiveData; JUnit 4 and Mockito. The repository uses Android Gradle Plugin **9.3.3**.

For design details, see [Architecture](docs/ARCHITECTURE.md) and [System Analysis](docs/system-analysis/README.md).

## Current limitations

- Only Woolworths/Countdown and PAK'nSAVE formats have registered parsers; layouts, OCR accuracy and printed-total recognition can vary. Users must review results before saving.
- Purchase dates currently default to processing time rather than reliably extracting the date printed on a receipt; this can affect historical Analytics.
- Data is device-local. There is no user account, cloud backup/restore, remote price feed, or notification service.
- Persistence across ordinary app restarts has been manually confirmed; a previous empty-data observation around emulator/instrumentation testing remains unproven as to root cause. Use a separate emulator for destructive or instrumentation testing.

## Development roadmap

The following are **plans, not shipped functionality**, and will be developed in stages:

1. **Receipt data quality:** add editable printed purchase dates and improve OCR/layout reconstruction and parsers using anonymized real receipts from additional NZ supermarkets (including New World and Four Square). Add regression fixtures for pricing, discounts, quantities, and totals.
2. **Analytics improvements:** compare months, explore store/category trends and item price history after receipt extraction is reliable.
3. **Automated reliability:** strengthen unit, Room and end-to-end tests, including save → restart → History → Analytics → delete. Isolate instrumented tests in a dedicated emulator and check persistence explicitly.
4. **Accounts and backup/restore:** only after the local app is stable, design registration, authentication, opt-in upload, restore and secure server storage. Plan data export/deletion and privacy controls before implementing collection.
5. **Crowdsourced price intelligence:** with user consent, normalize product identity, units, store, location, and observation time across uploaded receipts. Compare like-for-like prices and notify users when a verified current price is lower than their previous purchase price. Receipt observations alone are historical, so price freshness, data quality and sufficient observations must be validated before calling a price “current” or triggering alerts.

**Privacy direction:** keep receipt information local by default until the user explicitly enables a future backup/upload feature. Minimize personal data; protect records in transit and at rest; define access controls, retention, deletion, and opt-in notifications before launching a backend. No server endpoints or account credentials are required to run the present app.

## Contributing / local development

Work on a feature branch and stage only intended source and documentation files. Keep IDE settings (`.idea/`), `local.properties`, temporary patches, local databases, and diagnostic ZIPs out of commits. Run the JVM suite and debug build, then manually verify affected workflows before pushing. Do not change Room schemas without a matching versioned migration and test.

This project is maintained as a personal Android software-engineering learning and portfolio project.
