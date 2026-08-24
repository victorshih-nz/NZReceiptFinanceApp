# Validation Level & Cost Down Policy

**Version:** 1.1

This policy keeps validation strong without running the same expensive checks again and again.

The simple rule is: **use the lowest level that still covers the risk.** If the work becomes riskier, move up a level.

## How the pieces fit

There are three gates:

1. **Local validation** — controlled by L1–L4 in the Micro Job.
2. **GitHub Actions** — an independent remote check after an authorised PR/push.
3. **Human validation** — used for UI, device, persistence, or end-to-end behaviour when needed.

The Issue chooses the local Validation Level. CI does not trust that label; it checks the changed files itself.

## L1 — Documentation / Non-runtime

Use L1 for things such as:

- Markdown and design documents
- implementation plans
- Issue and PR templates
- `.github/copilot-instructions.md`

Local checks are light:

- `git status`
- `git diff --check`
- syntax/schema validation when the file format needs it

Do not run `testDebugUnitTest`, `assembleDebug`, or emulator tests by default.

A GitHub Actions workflow file is **not** L1. It changes the validation system, so treat it as L4.

## L2 — Logic / Unit-testable code

Typical examples:

- Domain logic and Use Cases
- parsers and validators
- calculations and paging logic
- logic-only ViewModel/state changes

Run:

- `git diff --check`
- focused unit tests for the changed behaviour

The full local `testDebugUnitTest` suite is optional unless the job changes a shared contract, has broad impact, or focused tests show wider risk.

`assembleDebug` is normally unnecessary if no Android resource, navigation, DI, Manifest, or build wiring changed.

The PR still gets the full Android CI gate.

## L3 — Android UI / Integration / DI

Use L3 for:

- Fragment or DialogFragment work
- RecyclerView adapters
- XML/resources
- Navigation
- View binding
- AppContainer / ViewModelFactory
- other Android wiring

Run:

- `git diff --check`
- focused tests when useful
- `assembleDebug`

Run the full local unit suite only when the job also changes shared logic or the Issue calls for it.

Visible UI or device-dependent changes need targeted manual Android validation.

## L4 — Persistence / Build / Cross-layer

Use L4 for higher-risk work, including:

- DAO / Room transaction behaviour
- repository persistence
- approved migrations
- Gradle or dependency changes
- cross-layer changes
- final integration/regression work
- GitHub Actions workflow changes

For normal runtime/persistence L4 work, run:

- `git diff --check`
- focused tests
- `testDebugUnitTest`
- `assembleDebug`

Then use GitHub Actions as the remote gate.

### CI workflow exception

A workflow-only change is still L4, but Android unit tests do not prove that YAML or GitHub Actions control flow is correct.

For that case, use:

- `git diff --check`
- YAML/workflow review
- an actual GitHub Actions run after the change is pushed

Use `workflow_dispatch` when you want to force a full Android CI run.

## Quick matrix

| Change | Level | Local validation | Remote CI | Manual |
|---|---|---|---|---|
| Docs / templates / Copilot instructions | L1 | diff + syntax | cheap check | No |
| Domain / parser / logic-only state | L2 | focused tests | full Android CI | Usually no |
| Fragment / Adapter / XML / Navigation / DI | L3 | focused + assemble | full Android CI | Usually yes |
| Room / persistence / Gradle / cross-layer | L4 | focused + full unit + assemble | full Android CI | When affected |
| GitHub Actions workflow only | L4 | workflow-specific checks | actual workflow run | No |

If a job fits two levels, use the higher one.

## CI cost control

Automatic Android CI runs on:

- pull requests targeting `main`
- pushes to `main`
- manual `workflow_dispatch`

Feature-branch pushes do not also trigger the same workflow. This avoids duplicate runs when a PR is already open.

For a PR, CI checks the **whole PR diff**, not only the latest commit.

If every changed file is clearly non-runtime — for example docs, Issue templates, the PR template, or Copilot instructions — the workflow stays green but skips JDK/Gradle work.

Anything else gets the full remote gate:

```text
testDebugUnitTest
assembleDebug
```

If change detection is uncertain, CI chooses the safer path and runs the full gate.

## Keep AI context small

A new Micro Job should normally start in a fresh agent conversation.

Use this order:

1. active Issue
2. referenced design section
3. expected files
4. direct callers/dependencies
5. focused tests

Do not scan the whole repo by default.

Successful command output should be short. For failures, look at the failed step and first useful error before opening more logs.

Avoid rereading large unchanged files. And do not rerun a full suite after every tiny edit — rerun the focused failure first, then finish the checks required by the level.

## Escalation and stop rules

Raise the level when the real change is riskier than expected. For example:

- L2 logic needs XML/Navigation → L3
- L3 UI work needs DAO changes → L4

Stop and ask for a Human/SA decision if the job unexpectedly needs:

- a Room schema migration
- a new external dependency
- a breaking interface change
- a major architecture change
- significant scope expansion

A higher Validation Level does not grant permission to make those changes.

## What to report

Keep the handoff brief:

- Micro Job ID and Validation Level
- focused validation result
- full unit / assemble result, or `NOT REQUIRED`
- CI status
- manual validation still needed
- changed files
- any escalation or unexpected finding

Do not paste full successful logs.

## Current History mapping

| Micro Job | Level |
|---|---|
| HIST-1.1 Receipt-only History | L3 |
| HIST-1.2 Receipt Paging | L3 |
| HIST-1.3 Receipt Delete Confirmation | L3 |
| HIST-2.1 Receipt Items State/Paging | L3 |
| HIST-2.2 Item Row Edit UI | L3 |
| HIST-2.3 Persist Item Edit | L4 |
| HIST-3.1 Add Item | L4 |
| HIST-3.2 Delete Item | L4 |
| SCAN-4.1 Preview Modal | L3 |
| SCAN-4.2 Preview Paging | L3 |
| SCAN-4.3 Preview Editable Fields | L3 |
| SCAN-4.4 Preview Save → History | L4 |
| SCAN-4.5 Preview Discard → Scan | L3 |
| SCAN-5.1 Date Fallback | L2 |
| HIST-6.1 Cross-flow Regression | L4 |
| HIST-6.2 Human E2E | Human |
