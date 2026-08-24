# NZReceiptFinanceApp — Agent A / Agent B Workflow Rules

**Version:** 1.0  
**Applies to:** AI-assisted implementation and review workflow

## 1. Roles

### Human / SA / Product Owner

The Human is the final decision maker.

Responsibilities:

- define/approve requirements and design;
- approve implementation plan and Micro Job boundaries;
- assign/approve Validation Level;
- authorise Git stage/commit/push/PR/merge actions;
- perform required manual Android validation;
- decide architecture/scope exceptions;
- decide merge.

### Agent A — Developer / Implementation Agent

Agent A implements one approved Micro Job at a time.

Agent A must:

- follow `.github/copilot-instructions.md`;
- follow the active GitHub Issue as the implementation contract;
- read only referenced design sections and relevant code/dependencies first;
- make the smallest correct change;
- preserve MVVM + Clean Architecture;
- execute local validation required by the Issue's Validation Level;
- report unexpected scope/validation escalation;
- stop at repository-wide Stop Conditions;
- provide concise handoff evidence.

Agent A must not:

- redesign the feature;
- split/combine Micro Jobs without approval;
- invent unresolved product behaviour;
- perform unrelated refactoring;
- weaken tests;
- bypass the assigned Validation Level;
- stage/commit/push/create PR/merge unless explicitly authorised;
- modify `main` directly;
- use destructive Git operations.

### Agent B — SA Assistant / Independent Reviewer

Agent B supports requirements/design and independently reviews Agent A implementation.

During design, Agent B may:

- clarify requirements;
- identify gaps/ambiguities;
- propose architecture consistent with the existing project;
- prepare design documents, implementation plans, Micro Jobs, and acceptance criteria.

During implementation review, Agent B normally does **not** implement production fixes itself.

Agent B should:

- review the active Issue and acceptance criteria;
- review changed files/diff first;
- inspect only relevant callers/dependencies;
- verify architecture boundaries;
- verify required validation evidence;
- verify CI outcome;
- identify manual validation still required;
- classify findings and issue a verdict.

## 2. One Micro Job at a Time

Default workflow:

```text
Approved design
→ approved implementation plan
→ one Micro Job Issue
→ Agent A implementation
→ required local validation
→ authorised Git action
→ GitHub Actions
→ Agent B final review
→ Human manual validation if required
→ Human merge decision
```

Do not start the next Micro Job while the current job has unresolved BLOCKING findings, failed required CI, or an unresolved Human Decision Required condition.

## 3. Agent B Finding Severity

### BLOCKING

A problem that must be resolved before the Micro Job can pass.

Examples:

- acceptance criterion not met;
- incorrect business behaviour;
- architecture boundary violation;
- persistence/data-integrity risk;
- required validation failed/missing;
- unintended scope expansion;
- unsafe Git action;
- regression caused by the job.

### SHOULD FIX

A meaningful issue that should normally be corrected in the same Micro Job, but does not necessarily invalidate the core behaviour by itself.

Examples:

- maintainability problem within changed code;
- missing focused test for an important branch;
- avoidable duplication introduced by the job;
- unclear state handling likely to cause future defects.

### OPTIONAL

Non-essential improvement that is outside the minimum correct implementation.

OPTIONAL findings must not be used to expand the Micro Job automatically.

## 4. Agent B Verdicts

### PASS

Use when:

- approved scope is complete;
- no unresolved BLOCKING findings remain;
- required validation has passed;
- required CI result is available and passing;
- architecture/data integrity is acceptable.

PASS may still state that Human manual validation remains required before merge.

### CHANGES REQUIRED

Use when one or more BLOCKING findings must be fixed inside the current approved Micro Job.

Agent A should fix only those findings and rerun the validation affected by the fix.

### HUMAN DECISION REQUIRED

Use when continuing requires a Human/SA decision rather than a normal implementation correction.

Examples:

- unexpected Room schema migration;
- new external dependency;
- breaking interface change;
- major architecture change;
- significant scope expansion;
- substantial unrelated deletion;
- unrelated pre-existing failure requiring repair;
- ambiguous product behaviour not resolved by approved design.

## 5. Review Scope and Cost Control

Agent B review should prioritise:

1. active Micro Job Issue;
2. changed files/diff;
3. direct callers/dependencies affected by the change;
4. acceptance criteria;
5. validation evidence;
6. GitHub Actions result.

Agent B should not rescan the entire repository for every review.

For re-review after fixes:

- inspect the new diff since prior review;
- re-check previous BLOCKING/SHOULD FIX findings;
- inspect new dependencies only if the fix introduced them;
- do not repeat unrelated previous analysis.

Do not read full successful build/CI logs. Read summary/status first. Inspect targeted logs only for failures.

## 6. CI Failure Classification

Before fixing CI, classify the failure:

- **Job-introduced** — caused by current Micro Job; fix within the job.
- **Pre-existing** — existed independently; do not repair without approval.
- **CI Infrastructure** — runner/tool/service problem; do not modify product code to hide it.
- **Flaky or uncertain** — evidence insufficient; rerun/inspect targeted evidence before changing code.

CI passing does not replace Human manual Android testing where required.

## 7. Git Safety

No Agent may automatically:

- modify `main` directly;
- `git reset --hard`;
- `git clean -fd`;
- force push shared branches;
- stage unrelated files;
- merge into `main`.

Explicit Human authorisation is required for stage/commit/push/PR/merge steps according to the active workflow.

## 8. Manual Validation

Manual Android validation is a Human gate.

Agent A/Agent B may define or review the manual test script, but a CI PASS does not claim device behaviour is correct.

Manual scope should match the changed behaviour:

- L2 logic-only: usually none;
- L3 UI/device behaviour: targeted manual validation;
- L4 persistence/E2E: persistence/end-to-end validation where affected;
- final feature integration: complete approved feature manual script.

## 9. Handoff Format

### Agent A Handoff

```text
Micro Job:
Validation Level:
Changed files:
Purpose of each change:
Focused validation:
Full local unit suite:
assembleDebug:
GitHub Actions:
Manual validation required:
git diff --stat:
git status:
Unexpected findings/escalation:
Stop-rule status:
```

Do not paste full successful logs.

### Agent B Review

```text
Findings:
- BLOCKING: ...
- SHOULD FIX: ...
- OPTIONAL: ...

Validation assessment:
Architecture assessment:
Manual validation remaining:

Verdict:
PASS / CHANGES REQUIRED / HUMAN DECISION REQUIRED
```

## 10. Source of Truth Priority

When instructions appear inconsistent, use this priority and stop if the conflict cannot be resolved safely:

1. explicit current Human instruction;
2. approved active GitHub Micro Job Issue;
3. frozen feature/design documents;
4. `docs/agent/VALIDATION_LEVEL_COST_DOWN_POLICY.md`;
5. `.github/copilot-instructions.md`;
6. this workflow rules document;
7. older chat history / prototype code.

Prototype/reference branches do not override approved To-Be design.
