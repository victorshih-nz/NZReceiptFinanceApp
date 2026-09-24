# Analytics dashboard

Analytics uses the stored **purchase date**, not the save time. Receipts without a purchase date are excluded from dated analytics; they may still appear in History.

## Navigation

The default view is January–December of the current calendar year. Use **Years** to see annual totals, tap a bar to select a period, and double-tap a year or month to drill into months or days. In the daily view, use **Back** to return to the selected month. The chart scrolls horizontally for dates outside the visible width. Pull down to refresh analytics. History refreshes when its screen resumes, including after saving a receipt.

## Financial contract

- **Total spending:** sum of `Receipt.getFinalPayableCents()` for the selected period.
- **Category spending:** sum of each `ReceiptItem.getFinalSubtotalCents()` grouped by stable category ID; an item with no category appears as **Uncategorized**.
- **Category percentage:** category cents divided by the sum of item/category cents, not by total spending.
- Receipt-level discounts are included in total spending but are **not allocated to categories**. A difference between category totals and total spending is therefore expected.
- Values are calculated in integer NZD cents and formatted with two decimal places.
- Time ranges use an inclusive start and exclusive end. Monthly series include all 12 months, and daily series include every day of the month, including zero-spend periods and February 29 in leap years.

## Architecture and verification

`AnalyticsFragment` renders `AnalyticsUiState`; `AnalyticsBarChartView` handles drawing and gestures. `AnalyticsViewModel` owns selection and delegates background queries to `GetAnalyticsUseCase`. The use case requests bounded receipts through `IReceiptRepository`, `ReceiptRepositoryImpl`, and Room `ReceiptDao`. No Analytics-specific database schema or migration is required.

Run the JVM suite and debug build:

```powershell
.\\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```

Run the Room instrumentation tests on an attached emulator or device:

```powershell
.\\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.nzreceiptapp.data.local.dao.AnalyticsReceiptDaoTest" --console=plain
```

Manual regression: save a new receipt and revisit History without manually refreshing; check Receipts and All Items; check the monthly chart, a nonzero daily bar beyond day 7, year/month double-tap, Back, and empty periods. Verify that the displayed total and category subtotal may differ when receipt-level discounts exist. Do not clear app data as part of testing existing receipts.
