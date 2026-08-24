# Micro Job Pull Request

## Micro Job

- **Micro Job ID:**
- **Parent Feature / Batch:**
- **GitHub Issue:**
- **Validation Level:** L1 / L2 / L3 / L4

## Goal

<!-- One concise statement of the approved Micro Job outcome. -->


## Scope Completed

<!-- Check only work that belongs to the approved Issue. -->

- [ ] Approved In-Scope work is complete.
- [ ] No Out-of-Scope behaviour was added.
- [ ] No unresolved product/architecture decision was delegated to implementation.

## Changed Files

| File | Purpose |
|---|---|
|  |  |

## Architecture / Dependency Notes

<!-- State only relevant architecture impact. Example: Presentation → Domain ← Data; Receipt aggregate update path preserved. -->

- Architecture boundaries preserved: Yes / No / N/A
- New abstraction/layer introduced: No / Yes — explain below
- Public/breaking interface changed: No / Yes — HUMAN DECISION REQUIRED unless pre-approved
- Room schema/migration changed: No / Yes — HUMAN DECISION REQUIRED unless pre-approved
- New external dependency added: No / Yes — HUMAN DECISION REQUIRED unless pre-approved

## Validation Evidence

Follow `docs/agent/VALIDATION_LEVEL_COST_DOWN_POLICY.md` and the Validation Level assigned in the Issue.

### Local

- `git diff --check`: PASS / FAIL
- Focused validation: PASS / FAIL / N/A
  - Command/test:
- `testDebugUnitTest`: PASS / FAIL / NOT REQUIRED
- `assembleDebug`: PASS / FAIL / NOT REQUIRED

Do not paste full successful logs. Include only concise results or relevant failure excerpts.

### GitHub Actions

- Android CI: PASS / FAIL / NOT RUN YET
- Failure classification if applicable: Job-introduced / Pre-existing / CI Infrastructure / Flaky or uncertain

## Manual Android Validation

- Required: Yes / No
- Status: PASS / FAIL / NOT RUN YET / NOT REQUIRED
- Checks performed:
  - 

## Agent B Review

- Verdict: PENDING / PASS / CHANGES REQUIRED / HUMAN DECISION REQUIRED
- BLOCKING findings resolved: Yes / No / N/A
- SHOULD FIX findings resolved: Yes / No / N/A

## Unexpected Findings / Validation Escalation

<!-- State "None" if there were no unexpected findings. -->

None

## Git Safety

- [ ] No destructive Git operations were used.
- [ ] No unrelated existing changes were modified.
- [ ] Only explicitly authorised paths were staged/committed.
- [ ] No force push was used.
- [ ] This PR does not merge itself into `main`.

## Human Merge Gate

This PR is ready for Human merge decision only after all applicable gates are complete:

- [ ] Approved Micro Job scope completed.
- [ ] Required local validation passed.
- [ ] Required GitHub Actions passed.
- [ ] Agent B final review passed.
- [ ] Required Human manual validation passed.

**Do not merge automatically. Human approval is required.**
