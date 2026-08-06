# NZ Receipt Expense Tracker

Android prototype for scanning New Zealand supermarket receipts, converting OCR
text into structured purchase data, and browsing saved receipts.

## Current status

Working foundations:

- Camera/gallery image input with Google ML Kit OCR
- Rule-based receipt parsers for Woolworths/Countdown and PAK'nSAVE
- Room persistence for stores, receipts, items, discounts, and categories
- Receipt history, item history, receipt details, and deletion
- Unit tests for parsers, use cases, domain logic, and ViewModels

Still in progress:

- The scanner currently assumes Woolworths and a placeholder branch name
- Analytics navigation exists, but its screen is not implemented
- Category seed data is not yet connected to application startup
- New World and Four Square parsers are not implemented
- Error messages are basic and are not yet modelled as one-time UI events

## Architecture

The project uses MVVM in the presentation layer and Clean Architecture dependency
rules across a single Android module:

```text
presentation  ->  domain  <-  data
       ^                      ^
       |                      |
       +------ di/app --------+
```

- `domain`: business models, repository/service contracts, and use cases. It has
  no Android framework imports.
- `data`: Room, ML Kit, parser implementations, repository implementations, and
  mapping between database and domain models.
- `presentation`: Fragments, adapters, ViewModels, and observable UI state.
- `di`: the application composition root. It is the only package that wires
  concrete data implementations to domain contracts and ViewModels.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for package ownership and the
rules to follow when adding a feature. See
[docs/REFACTORING_PLAN.md](docs/REFACTORING_PLAN.md) for the staged cleanup plan.

## Development setup

- Java source compatibility: 11
- Minimum Android SDK: 26
- Target Android SDK: 36
- Build system: Gradle Wrapper

Clone and run the local unit tests:

```bash
git clone https://github.com/victorshih-nz/NZReceiptFinanceApp.git
cd NZReceiptFinanceApp
./gradlew testDebugUnitTest
```

Open the project in Android Studio to run the app on an emulator or Android
device. The first Gradle sync requires network access to download the configured
Gradle and Android dependencies.
