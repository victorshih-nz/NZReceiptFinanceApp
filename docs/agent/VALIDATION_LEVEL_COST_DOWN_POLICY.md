# NZReceiptFinanceApp — Validation Level & Cost Down Policy

**Document ID:** VCP-001  
**Version:** 1.0 — Approved  
**Purpose:** Reduce unnecessary AI token usage, local compute, and CI compute without weakening engineering assurance.  
**Applies to:** Agent A implementation work, Agent B review, GitHub Actions, and Human manual validation.

---

## 1. Core Principle

Use the **lowest sufficient local Validation Level** for each Micro Job.

Do not mechanically duplicate expensive validation when another authoritative gate already covers the same risk.

The policy controls three different costs:

1. AI context/token cost
2. Local developer compute/time
3. GitHub Actions compute/minutes

The goal is:

> **minimum unnecessary compute, not minimum engineering assurance.**

---

## 2. Three-Layer Validation Model

The project uses three cooperating layers.

```text
Layer 1 — Micro Job Validation Level
          controls Agent A local validation

Layer 2 — GitHub Actions Remote Gate
          independently checks repository changes after authorised push/PR

Layer 3 — Human Validation
          checks user-visible/device/end-to-end behaviour when required
```

The GitHub Issue assigns L1/L2/L3/L4.

GitHub Actions does **not** trust or infer that Issue level. It independently checks the changed files as a safety net.

---

## 3. Standard Micro Job Lifecycle

```text
Approved Micro Job
    ↓
Agent A implementation
    ↓
Local validation required by L1/L2/L3/L4
    ↓
Agent A reports ready for authorised Git action
    ↓
Human authorises stage / commit / push / PR as applicable
    ↓
GitHub Actions remote gate
    ↓
Agent B final review
    ↓
Human manual Android validation when required
    ↓
Human decides merge
```

Agent B may perform an optional pre-push diff review, but the **final Agent B verdict is after the required CI result is available**.

GitHub Actions success does not replace Human manual validation.

---

# 4. Validation Levels

Every implementation Micro Job must declare exactly one level:

```text
L1 — Documentation / Non-runtime Configuration
L2 — Logic / Unit-testable Code
L3 — Android Integration / UI / DI
L4 — Persistence / Build / Cross-layer High Risk
```

If more than one level applies, use the highest applicable level.

If implementation unexpectedly requires a higher level, Agent A must report the escalation.

If the escalation crosses a Stop Condition or approved scope boundary:

```text
HUMAN DECISION REQUIRED
```

---

# 5. L1 — Documentation / Non-runtime Configuration

## Typical Changes

- Markdown documentation
- design documents
- implementation plans
- GitHub Issue templates
- Pull Request templates
- `.github/copilot-instructions.md`
- non-executable repository guidance/metadata

A comment-only change inside a runtime source file may be logically L1, but GitHub Actions may still conservatively treat the source file as runtime-relevant because CI classification is path-based.

The following are not L1:

- GitHub Actions workflow files
- Gradle/build files
- dependency configuration
- Android resources
- Room configuration/schema

## Required Local Validation

```text
git status
git diff --check
syntax/schema validation when applicable
```

Examples:

- Issue Form YAML syntax/schema
- JSON syntax
- Markdown structural review

Do not install a new dependency solely for a lightweight syntax check.

## Not Required Locally by Default

```text
testDebugUnitTest
assembleDebug
Android emulator/device test
```

## Remote CI Behaviour

For allow-listed non-runtime-only changes, GitHub Actions still produces the normal CI check but skips JDK/Gradle work after a cheap changed-file classification step.

If the change touches a path not safely classified as non-runtime, remote CI runs full Android validation.

---

# 6. L2 — Logic / Unit-testable Code

## Typical Changes

- Domain logic
- Use Cases
- validators
- parser logic
- calculations
- paging calculations
- mapping logic
- immutable UI-state logic
- ViewModel logic with no Android resource/navigation/DI changes
- repository logic that does not alter DAO/schema or broad persistence semantics

## Required Local Validation

```text
git status
git diff --check
focused unit test(s)
```

The focused test must cover the changed behaviour.

## Full Local Unit Suite

`testDebugUnitTest` is conditional.

Run it locally when:

- a shared contract changed;
- many components depend on the change;
- focused tests expose broader regression risk;
- the Micro Job explicitly requires it;
- actual scope expanded within the approved job;
- GitHub Actions is unavailable.

Otherwise the remote CI gate performs the full unit suite.

## Local assembleDebug

Normally not required when no Android resource, Manifest, Navigation, DI or build wiring changed.

## Remote CI Behaviour

A runtime-relevant L2 PR receives:

```text
testDebugUnitTest
assembleDebug
```

in GitHub Actions.

## Manual Validation

Usually not required unless the logic has important user-visible behaviour not adequately covered by tests.

---

# 7. L3 — Android Integration / UI / DI

## Typical Changes

- Fragment
- DialogFragment/modal
- RecyclerView Adapter
- XML layout/resources
- Navigation
- View binding
- AppContainer
- ViewModelFactory
- presentation-domain wiring
- lifecycle behaviour
- CameraX/Gallery behaviour
- UI state wired into Android components

## Required Local Validation

```text
git status
git diff --check
focused tests when applicable
assembleDebug
```

`assembleDebug` is required because Android resource/binding/navigation/DI errors can escape focused JVM tests.

## Full Local Unit Suite

Conditional.

Run locally when:

- shared ViewModel/domain behaviour changes;
- the job spans significant presentation + domain logic;
- focused tests indicate wider risk;
- the Issue explicitly requires it.

Otherwise remote CI supplies the full unit suite.

## Remote CI Behaviour

A runtime-relevant L3 PR receives full Android CI:

```text
testDebugUnitTest
assembleDebug
```

## Human Manual Validation

Required for user-visible/device-dependent changes such as:

- layout
- pagination controls
- edit/save/delete icons
- modal behaviour
- navigation
- RecyclerView state
- CameraX/Gallery

---

# 8. L4 — Persistence / Build / Cross-layer High Risk

## Typical Changes

- DAO/Room transaction behaviour
- repository persistence semantics
- entity relationship behaviour
- approved Room migration work
- Gradle/build configuration
- dependency configuration
- Android Gradle Plugin changes
- GitHub Actions workflow changes
- build scripts
- Presentation + Domain + Data integration
- broad shared interface changes
- final integration/regression jobs

## Required Local Validation — Runtime/Persistence L4

Normally:

```text
git status
git diff --check
focused test(s)
testDebugUnitTest
assembleDebug
```

Then GitHub Actions is the authoritative remote gate.

## L4 Special Case — CI/Workflow-only Changes

A GitHub Actions workflow change is L4 because it changes the validation infrastructure itself.

However, Android tests/builds do not validate YAML/control-flow correctness by themselves.

For a workflow-only Micro Job, use:

```text
git status
git diff --check
workflow/YAML review or syntax validation
actual GitHub Actions run after authorised push
```

Use `workflow_dispatch` where a full explicit run is needed.

Run local Android unit/build tasks only when the workflow change also changes Android build behaviour and the Micro Job specifically requires local reproduction.

## Human Manual Validation

Required when L4 affects:

- persisted user data;
- user-visible data;
- runtime/device behaviour;
- end-to-end application flow.

Workflow-only L4 normally does not require Android manual UI testing.

## Stop Conditions Still Apply

L4 does not automatically authorise:

- unexpected Room schema migration;
- new external dependency;
- breaking public interface change;
- major architecture redesign;
- substantial unrelated deletion.

Those require Human/SA decision.

---

# 9. Validation Matrix

| Change type | Level | Focused local | Full local unit | Local assemble | Remote GitHub Actions | Human manual |
|---|---:|---:|---:|---:|---|---:|
| Markdown/design docs | L1 | No | No | No | Cheap check; Gradle skipped | No |
| Issue / PR template | L1 | No | No | No | Cheap check; Gradle skipped | No |
| Copilot instructions | L1 | No | No | No | Cheap check; Gradle skipped | No |
| Pure Domain / Use Case | L2 | Yes | Conditional | Usually No | Full Android CI | Usually No |
| Parser logic | L2 | Yes | Conditional | Usually No | Full Android CI | Conditional |
| Logic-only ViewModel/state | L2 | Yes | Conditional | Usually No | Full Android CI | Conditional |
| Fragment / Adapter | L3 | When applicable | Conditional | Yes | Full Android CI | Yes |
| XML / Android resource | L3 | When applicable | Conditional | Yes | Full Android CI | Yes |
| Navigation | L3 | When applicable | Conditional | Yes | Full Android CI | Yes |
| AppContainer / ViewModelFactory | L3 | Yes when possible | Conditional | Yes | Full Android CI | Conditional |
| DAO / Room transaction | L4 | Yes | Yes | Yes | Full Android CI | Yes |
| Approved Room migration | L4 | Yes | Yes | Yes | Full Android CI | Yes |
| Gradle / dependency | L4 | Relevant | Yes | Yes | Full Android CI | Conditional |
| GitHub Actions workflow only | L4 | Workflow-specific | Usually No | Usually No | Actual workflow run | No |
| Cross-layer integration | L4 | Yes | Yes | Yes | Full Android CI | Yes |

---

# 10. GitHub Actions Cost-Control Policy

## 10.1 Remote CI Does Not Implement L1-L4 Directly

The Issue level controls local Agent A work.

CI uses a simpler independent classification:

```text
Non-runtime-only change
        ↓
cheap CI check
        ↓
skip JDK / Gradle

Any runtime-relevant or uncertain change
        ↓
full Android CI
        ↓
testDebugUnitTest
assembleDebug
```

This keeps CI conservative and prevents an incorrect Issue level from weakening the remote gate.

## 10.2 PR Diff Is the Primary Feature-branch Gate

For pull requests targeting `main`, changed-file classification must consider the **entire PR diff against the base**, not only the most recent commit.

This prevents a docs-only follow-up commit from hiding earlier runtime changes in the same PR.

## 10.3 Avoid Duplicate Push + PR Runs

Automatic CI should run on:

- `pull_request` targeting `main`;
- `push` to `main`;
- `workflow_dispatch`.

Do not automatically run the same Android CI on every feature-branch `push` **and** again on `pull_request`, because an open PR can otherwise trigger duplicate runs for the same work.

If a feature branch has no PR yet and CI is needed, use `workflow_dispatch` or open the intended Draft PR.

## 10.4 Why Not paths-ignore

Do not use top-level `paths-ignore` for docs-only optimisation.

The workflow itself should still create a successful check for non-runtime-only changes, which is safer if the CI check later becomes required by branch protection.

## 10.5 Conservative Classification

CI classification is path-based, not semantic.

If CI cannot reliably determine that a change is non-runtime, it runs full Android validation.

This intentionally prefers occasional extra compute over a false skip.

---

# 11. Successful and Failed CI Logs

## Successful CI

Normally inspect only:

- workflow conclusion;
- job conclusion;
- step conclusions.

Do not read/copy full successful Gradle logs into AI context.

## Failed CI

Inspect progressively:

1. failed step;
2. first relevant error;
3. small surrounding log range;
4. expand only if needed.

Classify as:

- Job-introduced
- Pre-existing
- CI Infrastructure
- Flaky or uncertain

Fix only Job-introduced failures inside the active Micro Job unless explicitly authorised otherwise.

---

# 12. AI Context / Token Cost Policy

## 12.1 One Micro Job = One Fresh Agent A Conversation

Default:

```text
New Micro Job
    ↓
New Agent A conversation
```

Do not begin a new job inside a long prior implementation chat.

## 12.2 Project Memory Hierarchy

```text
Repository instructions / approved design = long-term memory
GitHub Issue                              = current job contract
Agent conversation                        = temporary working memory
```

## 12.3 Minimal Startup Prompt

After Job 0.3:

```text
Implement GitHub Issue #NN.

Follow .github/copilot-instructions.md.
Follow the Validation Level assigned in the Issue.
Read only the referenced design sections and relevant files/dependencies.
Work only on this Micro Job.
Do not commit or push unless authorised.
```

## 12.4 Read Relevant Files Only

Start with:

1. active Issue;
2. referenced design sections;
3. expected components;
4. direct callers/dependencies;
5. focused tests.

Do not scan the whole repository by default.

Expand only for:

- direct dependency need;
- failing test;
- architecture uncertainty;
- Stop-rule risk.

## 12.5 Successful Output Is Summarised

Prefer:

```text
PASS
tests: 8
exit code: 0
```

over full successful logs.

For failures, inspect targeted error context first.

---

# 13. Context Window Guidance

Operational guidance:

- below ~40%: normal;
- ~40–60%: avoid unnecessary large file/log reads;
- above ~60%: compact if the same Micro Job must continue;
- above ~70%: strongly prefer compact or controlled fresh continuation.

Regardless of remaining context, a new Micro Job should normally start in a fresh conversation.

---

# 14. Command and Test Re-run Policy

Prefer concise commands:

```text
git status --short
git diff --stat
git diff --check
path-scoped git diff
```

Do not repeatedly print full repository diffs.

When fixing a failing test:

```text
edit
→ rerun failed/focused test
→ once focused test passes
→ run remaining validation required by the assigned level
```

Do not rerun expensive full validation after a documentation-only follow-up edit.

---

# 15. Agent A Inspection Budget

A normal Micro Job should be solvable from:

```text
Issue
+
referenced design sections
+
expected files
+
direct callers/dependencies
+
focused tests
```

If broad repository inspection is believed necessary, Agent A should first report:

```text
Why broader inspection is needed
Files/areas to inspect
Risk if not inspected
```

---

# 16. Agent B Review Cost Policy

Agent B prioritises:

1. changed files;
2. relevant caller/dependency relationships;
3. acceptance criteria;
4. required local validation evidence;
5. GitHub Actions result.

For re-review:

- inspect only the new diff since the previous review;
- re-check previous BLOCKING/SHOULD FIX findings;
- avoid repeating unrelated inspection.

Final Agent B verdict occurs after required CI is available.

---

# 17. Human Manual Validation Cost Policy

Manual testing is proportional to changed behaviour.

Examples:

### L2 Domain-only
Usually no device test.

### L3 Pagination UI
Test only relevant pagination/UI behaviour, not unrelated CameraX flows.

### L4 Final integration
Run the complete feature-level manual validation script.

---

# 18. Model Cost Principle

When model selection is available:

Use a smaller/faster capable model for:

- L1 mechanical work;
- simple YAML/Markdown;
- straightforward L2 work;
- clear repetitive fixes.

Use stronger reasoning/coding models for:

- architecture-sensitive work;
- difficult cross-layer debugging;
- L4 persistence/build problems;
- complex Agent B review;
- ambiguous failures.

Do not hard-code model names because availability/pricing can change.

---

# 19. Validation Escalation Rules

Examples:

```text
L1 docs job unexpectedly touches workflow file → L4-CI
L2 logic job requires XML/navigation change     → L3
L3 UI job requires DAO transaction change       → L4
Room schema migration unexpectedly required     → STOP
new external dependency unexpectedly required   → STOP
major architecture redesign required            → STOP
```

Agent A must report escalation before performing substantial work outside the expected validation scope.

---

# 20. Validation Evidence in Micro Job Report

```text
Micro Job:
Validation Level:

Focused validation:
PASS / FAIL / N/A

Full local unit suite:
PASS / FAIL / NOT REQUIRED

assembleDebug:
PASS / FAIL / NOT REQUIRED

GitHub Actions:
PASS / FAIL / NOT RUN YET

Manual validation:
REQUIRED / NOT REQUIRED

git diff --check:
PASS / FAIL

Changed files:
[...]

Unexpected validation escalation:
None / [...]
```

Do not include full successful logs.

---

# 21. Final Acceptance Principle

A Micro Job is accepted only when all applicable gates are satisfied:

```text
Approved scope completed
+
required local validation passes
+
authorised Git operation completed
+
required GitHub Actions gate passes
+
Agent B final review passes
+
Human manual validation passes when required
```

---

# 22. History Micro Job Level Mapping

| Micro Job | Level | Reason |
|---|---:|---|
| HIST-1.1 Receipt-only History State | L3 | History presentation + ViewModel/UI wiring |
| HIST-1.2 Receipt Paging Defaults and Controls | L3 | Android paging controls + state |
| HIST-1.3 Receipt Delete Confirmation | L3 | confirmation UI + existing delete wiring |
| HIST-2.1 Receipt Items State and Paging | L3 | Receipt Items state plus paging UI contract |
| HIST-2.2 Read-only / Edit Item Row UI | L3 | Adapter/row UI behaviour |
| HIST-2.3 Persist Item Edit | L4 | UI + domain aggregate update + persistence |
| HIST-3.1 Add Item | L4 | UI + persisted aggregate mutation |
| HIST-3.2 Delete Item | L4 | persisted aggregate mutation + paging |
| SCAN-4.1 Preview Modal | L3 | modal/navigation/UI |
| SCAN-4.2 Preview Item Paging | L3 | Preview state + UI controls |
| SCAN-4.3 Preview Editable Fields | L3 | UI/Adapter/validation integration |
| SCAN-4.4 Preview Save → History | L4 | persistence + navigation + cross-flow |
| SCAN-4.5 Preview Discard → Scan | L3 | Scanner state + modal/navigation |
| SCAN-5.1 Receipt Date Fallback | L2 | parser/domain logic if implementation stays logic-only |
| HIST-6.1 Cross-flow Regression | L4 | final integration gate |
| HIST-6.2 Human E2E Validation | Human | manual acceptance gate |

If SCAN-5.1 unexpectedly requires Android or persistence wiring, escalate its level accordingly.

---

# 23. Policy Status

**Validation Levels:** Defined  
**Local/remote gate relationship:** Defined  
**AI cost controls:** Defined  
**CI cost controls:** Defined  
**History mapping:** Defined  
**Approved for Agent A use:** Yes
