# Refactoring plan

The project already has a useful `domain / data / presentation` skeleton. The
safe approach is to close architecture gaps feature by feature instead of moving
every file at once.

## Phase 1 — stable MVVM boundary

- [x] Add an application-level composition root (`AppContainer`).
- [x] Move dependency construction out of the presentation package.
- [x] Add `ReceiptDetailViewModel`; remove Room/repository access from its Fragment.
- [x] Inject an `Executor` into ViewModels instead of creating unmanaged threads.
- [x] Repair stale History ViewModel tests and remove `Thread.sleep` calls.
- [x] Document the real implementation status and dependency rules.

## Phase 2 — predictable UI state

- [ ] Replace separate loading/data/error LiveData fields with one immutable UI
  state per screen.
- [ ] Model Toast/navigation messages as one-time events.
- [ ] Prevent duplicate loads when the History screen and tabs initialise.
- [ ] Stop pagination at the final page and expose retry actions.
- [ ] Move hard-coded labels, chain names, and branch names into resources/state.

## Phase 3 — receipt capture workflow

- [ ] Add a review/edit screen between parsing and saving.
- [ ] Detect or ask for supermarket chain instead of hard-coding Woolworths.
- [ ] Separate OCR output, parser warnings, and validation failures.
- [ ] Add New World and Four Square only after shared parser fixtures are defined.
- [ ] Connect category seed data and define a repeatable migration strategy.

## Phase 4 — data quality and analytics

- [ ] Add Room repository integration tests.
- [ ] Define rounding and discount invariants in domain tests.
- [ ] Implement analytics with a domain result model and dedicated ViewModel.
- [ ] Add export/backup and, later, cloud sync behind new repository contracts.

## Avoid for now

- Splitting into multiple Gradle modules before features and boundaries stabilise
- Adding a DI framework only to replace the small manual container
- Rewriting Java to Kotlin during architecture cleanup
- Renaming every interface/class in the same change as behavioural work
