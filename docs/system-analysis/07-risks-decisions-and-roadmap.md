# 07 — Risks, Decisions, and Roadmap

## 1. Risk register

Likelihood and impact are qualitative for the current learning project.

| ID | Risk | Likelihood | Impact | Current control | Recommended action |
| --- | --- | --- | --- | --- | --- |
| RISK-01 | OCR splits item names and prices or misreads characters | High | High | Coordinate row builder and review | Expand anonymised image/fragment fixtures |
| RISK-02 | Supermarket format change breaks deterministic parser | High | High | Chain-specific parser tests | Version fixtures; monitor failures; add warnings |
| RISK-03 | Printed/calculated total mismatch is saved without resolution | Medium | High | Visible warning and review | Define blocking/tolerance policy |
| RISK-04 | Receipt purchase date is wrong because processing time is used | High | Medium | User can see receipt data, but date is not editable | Parse and review purchase date |
| RISK-05 | Room schema changes without valid migration cause data loss/crash | Medium | High | Version 2 migration and schema JSON | Add automated migration tests and CI |
| RISK-06 | Concurrent or repeated scan actions produce stale UI callbacks | Medium | Medium | Buttons disabled while loading | Add operation ID/cancellation policy |
| RISK-07 | Category cache does not reflect runtime rule changes | Low now | Medium later | Rules mostly static per process | Add cache invalidation if rule editing is added |
| RISK-08 | Unbounded History Next produces confusing empty pages | High | Low | Previous guard | Return total/hasNext or fetch pageSize+1 |
| RISK-09 | Toast error repeats or is lost after lifecycle change | Medium | Medium | Last-message string | Introduce one-time UI events |
| RISK-10 | App-private images increase storage use indefinitely | Medium | Medium | User deletion removes image | Add storage usage/retention/export policy |
| RISK-11 | No CI allows broken code to merge into `main` | Medium | High | Manual local commands | Add GitHub Actions required checks |
| RISK-12 | Sample receipt contains personal or payment information | Medium | High | Development ownership | Anonymise fixtures and document provenance |
| RISK-13 | Nullable physical Store fields conflict with domain save rules | Low | Medium | Review validation | Tighten schema in future migration |
| RISK-14 | Analytics output misleads because categories or discounts are wrong | Medium | High | Analytics UI deferred | Define calculation/quality rules before UI |
| RISK-15 | Accessibility/touch targets are inadequate | Medium | Medium | Android lint warnings | Add accessibility audit and UI tests |

## 2. Architecture and product decisions

| ID | Decision | Rationale | Consequence |
| --- | --- | --- | --- |
| DEC-01 | Use one Android module with package boundaries | Lower learning/build overhead at current scale | Boundaries rely on discipline, not module compiler rules |
| DEC-02 | Use Java and Android Views/View Binding | Matches current project and learner focus | No Compose/Kotlin migration during foundation work |
| DEC-03 | Use MVVM for presentation | Separates lifecycle UI from orchestration/state | ViewModels and observable state require deliberate ownership |
| DEC-04 | Keep domain independent of Android/data | Enables JVM tests and implementation replacement | Requires contracts and mapping code |
| DEC-05 | Use manual dependency injection | Dependency graph is small and visible to learner | AppContainer/Factory must be maintained manually |
| DEC-06 | Require review before persistence | OCR/parser output is uncertain | One extra user step; better data integrity |
| DEC-07 | Store money as integer cents | Avoid currency floating-point storage error | Quantity remains decimal and subtotal is rounded |
| DEC-08 | Use chain-specific deterministic parsers | Explainable and testable for known formats | Fragile when formats change; fixtures are essential |
| DEC-09 | Classify after parsing in the use case | Parser owns format only; classification is shared | Every parser receives consistent category handling |
| DEC-10 | Rebuild OCR rows from coordinates | Flattened text loses name/price relationships | Layout algorithm becomes a testable processing stage |
| DEC-11 | Copy source images into app-private storage | External/cache URIs may expire | App owns storage lifecycle and usage |
| DEC-12 | Share Scanner ViewModel at Activity scope | Scanner and Review need the same draft | Must reset/discard state explicitly |
| DEC-13 | Persist raw OCR and printed total | Supports audit, troubleshooting, reprocessing | More local storage and privacy responsibility |
| DEC-14 | Reuse Store by unique chain/branch | Avoid duplicate Store rows | Normalisation/case policy must be defined later |
| DEC-15 | Defer Analytics UI | Receipt data quality is a prerequisite | Analytics destination remains visibly incomplete |

## 3. Technical debt register

| ID | Debt item | Effect | Suggested resolution |
| --- | --- | --- | --- |
| TD-01 | History state split across multiple LiveData fields | Possible inconsistent UI/loading | Add immutable `HistoryUiState` |
| TD-02 | Toast/navigation are not one-time events | Repetition/loss across lifecycle | Add event/effect model |
| TD-03 | Hard-coded UI strings remain in Java/XML | Localisation and consistency cost | Move to string resources |
| TD-04 | Analytics destination is empty | Confusing user experience | Hide until implemented or build feature |
| TD-05 | No page-end detection | Empty History pages | Add `hasNext` contract |
| TD-06 | No migration/instrumentation tests | Framework integration risk | Add Room/Espresso tests |
| TD-07 | Parser fixtures are embedded mainly as strings | Weak real-layout regression | Add anonymised structured fixtures |
| TD-08 | Category cache has no invalidation | Future rule editing inconsistency | Repository observable/versioned cache |
| TD-09 | Date is processing time | Incorrect historical reporting | Parse/edit printed date |
| TD-10 | Scanner Fragment manually copies test asset | Test-only responsibility in production UI | Decide debug-only sample strategy later |
| TD-11 | Some legacy models/adapters are not central to current priority | Cognitive overhead | Confirm usages before removal |
| TD-12 | No automated architecture dependency check | Layer violations can enter silently | Add static test/lint rule |

## 4. Prioritised roadmap

### Phase A — Reliability baseline

1. Add GitHub Actions for `testDebugUnitTest` and `assembleDebug`.
2. Add Room `MigrationTestHelper` coverage for v1→v2.
3. Add anonymised real receipt fixtures for both supported chains.
4. Define parser validation result: items, totals, warnings, and fatal errors.
5. Add purchase-date parsing and review.

**Exit criteria:** CI is required; database upgrades and supported fixtures pass;
the user can see and correct date and parser warnings.

### Phase B — Predictable presentation state

1. Introduce `HistoryUiState`.
2. Define page result with `hasNext`.
3. Introduce one-time UI effects for Toast/navigation.
4. Remove duplicate initial loads.
5. Move hard-coded labels to resources and resolve lint accessibility issues.

**Exit criteria:** History state cannot represent contradictory loading/data/error
combinations; final-page navigation is correct.

### Phase C — Analytics definition and delivery

1. Elicit user questions analytics must answer.
2. Define date range, category hierarchy, uncategorised handling, and discounts.
3. Confirm domain result model and acceptance examples.
4. Build Analytics ViewModel/UI and tests.
5. Add empty/loading/error and data-quality indicators.

**Exit criteria:** Every displayed value traces to an approved rule and test.

### Phase D — Parser expansion

1. Define minimum fixture set and accuracy acceptance for a new chain.
2. Add New World behind `IReceiptParser`.
3. Add Four Square behind `IReceiptParser`.
4. Add chain-specific warnings and regression fixtures.

**Exit criteria:** New parsers do not change existing parser/category behaviour.

### Phase E — Portability and lifecycle

1. Define export and backup requirements.
2. Implement export behind a domain contract.
3. Define image retention and storage reporting.
4. Consider optional cloud sync only after privacy/auth requirements exist.

## 5. Systems-analysis backlog

Before implementing each phase, the systems analyst should produce:

- stakeholder/user outcome;
- in-scope and out-of-scope statement;
- functional and non-functional requirements;
- updated use cases and alternate flows;
- business rules and example calculations;
- data changes and migration impact;
- wireframe or UI information requirements where relevant;
- acceptance tests and traceability updates;
- risks, assumptions, dependencies, and unresolved questions.

## 6. Decision process for future changes

Use this lightweight sequence:

1. **Problem:** What observed user/system problem exists?
2. **Evidence:** What code, test, feedback, or metric demonstrates it?
3. **Options:** What are at least two feasible responses, including no change?
4. **Trade-offs:** Compare correctness, usability, complexity, testability, data
   migration, privacy, and maintenance.
5. **Decision:** Record selected option and why.
6. **Consequences:** State new constraints, debt, and follow-up work.
7. **Verification:** Define how success will be observed.

This prevents solution-first development and is a core systems-analysis habit.
