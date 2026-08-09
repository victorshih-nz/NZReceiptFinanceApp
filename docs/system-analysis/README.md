# System Analysis Documentation

This directory is the controlled analysis baseline for the NZ Receipt Expense
Tracker. It documents the system that exists on `main` after PR #1, while clearly
marking planned behaviour that is not yet implemented.

## Document control

| Field | Value |
| --- | --- |
| System | NZ Receipt Expense Tracker |
| Baseline | `main` at `2cf8c90` plus documentation-only updates |
| Document version | 1.0 |
| Baseline date | 2026-08-10 |
| Status | As-Is baseline with identified To-Be gaps |
| Owner | Project maintainer |
| Intended readers | Developer, tester, systems analyst, reviewer |

## Document set

| Document | Main question answered |
| --- | --- |
| [01 — Context and scope](01-context-and-scope.md) | Why does the system exist, who uses it, and where is its boundary? |
| [02 — Requirements specification](02-requirements-specification.md) | What must the system do, and which requirements are implemented? |
| [03 — Use cases](03-use-cases.md) | How do actors achieve each goal, including alternate and failure flows? |
| [04 — Business rules and data](04-business-rules-and-data.md) | What rules govern receipts, totals, categories, and persistence? |
| [05 — Architecture and interfaces](05-architecture-and-interfaces.md) | How do components collaborate without breaking Clean Architecture? |
| [06 — Verification and traceability](06-verification-and-traceability.md) | How do requirements map to tests and acceptance evidence? |
| [07 — Risks, decisions, and roadmap](07-risks-decisions-and-roadmap.md) | What can go wrong, what was decided, and what should happen next? |
| [08 — Component catalogue](08-component-catalogue.md) | What is the responsibility of each Java source file? |

## Requirement and evidence identifiers

| Prefix | Meaning | Example |
| --- | --- | --- |
| `FR` | Functional requirement | `FR-OCR-02` |
| `NFR` | Non-functional requirement | `NFR-MNT-01` |
| `UC` | Use case | `UC-01` |
| `BR` | Business rule | `BR-TOTAL-01` |
| `T` | Automated test or test group | `T-PARSER-01` |
| `AT` | Manual acceptance test | `AT-SCAN-01` |
| `RISK` | Project or system risk | `RISK-03` |
| `DEC` | Architecture or product decision | `DEC-04` |

Identifiers are stable. Do not renumber existing entries when a requirement is
removed; mark it retired and record the reason instead.

## Status vocabulary

| Status | Meaning |
| --- | --- |
| Implemented | Behaviour exists in the current application |
| Partial | Some supporting code exists, but the complete user outcome does not |
| Planned | Agreed future scope; no complete implementation exists |
| Out of scope | Intentionally excluded from the current product boundary |

## Source-of-truth order

When documentation and code disagree, resolve the discrepancy in this order:

1. Reproducible automated or manual behaviour on the current `main` branch.
2. Current source code and Room schema JSON.
3. Requirements and business rules in this directory.
4. README and historical design files.

The mismatch must then be corrected in either code or documentation. Do not let
two conflicting versions remain undocumented.

## How a systems analyst should use this set

1. Start with context and scope before discussing solutions.
2. Convert stakeholder goals into uniquely identified requirements.
3. Use use cases to expose main, alternate, and exception paths.
4. Extract business rules and data definitions separately from UI details.
5. Check that architecture components satisfy requirements without crossing
   dependency boundaries.
6. Map every important requirement to test or acceptance evidence.
7. Record assumptions, unresolved questions, risks, and decisions.
8. Update all affected artefacts when scope or behaviour changes.

This is requirements traceability: each important need can be followed from
problem statement to requirement, use case, design component, and verification.

## Change-control checklist

For any behavioural change:

- identify the affected actor and stakeholder goal;
- update or add the relevant `FR`, `NFR`, `UC`, and `BR` entries;
- update the data dictionary if stored information changes;
- update sequence/state diagrams if interactions change;
- add or change automated and manual acceptance tests;
- record new risks or decisions;
- update README status and roadmap where necessary;
- include the documentation changes in the same pull request as the code.
