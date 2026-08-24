# Repository Copilot Instructions

These rules apply to every Micro Job in this repository.

## Start here

- Work on one Micro Job at a time.
- Read the active GitHub Issue first. It defines the job scope and task branch.
- Read only the design sections, files, tests, and direct dependencies needed for that job.
- Do not scan the whole repository unless there is a clear reason.
- Run `git status` before editing.
- If unexpected uncommitted changes already exist, do not discard or overwrite them. Stop and report `HUMAN DECISION REQUIRED`.
- Never modify `main` directly.

## Design and architecture

Keep the existing MVVM + Clean Architecture structure:

- presentation may depend on domain;
- data may depend on domain;
- domain must not depend on presentation, data, Android UI, or Room;
- AppContainer / ViewModelFactory remain the composition root.

Prefer the existing design before adding a new abstraction.

The frozen feature design defines product behaviour. The active Issue may narrow that design for one Micro Job, but it must not contradict it. If they conflict, stop and ask for a Human/SA decision.

Do not invent missing product behaviour.

## Scope

Make the smallest correct change.

Do not:

- split, combine, redesign, or expand the Micro Job without approval;
- refactor unrelated code;
- change unrelated formatting;
- fix unrelated failures;
- weaken or remove tests just to get a green result.

Inspect relevant callers and downstream dependencies before changing behaviour.

## Validation

Every Micro Job has one Validation Level.

- **L1** — lightweight checks only. No Android test/build by default.
- **L2** — focused unit tests. Full unit suite is conditional.
- **L3** — focused tests when useful, plus local `assembleDebug`. Manual Android validation is normally required for visible UI/device changes.
- **L4** — for runtime/persistence work, normally run focused tests, `testDebugUnitTest`, and `assembleDebug`. CI-workflow-only jobs use workflow-specific validation instead.

Always run `git diff --check` before handoff.

Use the validation written in the Issue. If more detail is needed, read only the relevant section of `docs/agent/VALIDATION_LEVEL_COST_DOWN_POLICY.md`; do not reread the whole policy for every job.

GitHub Actions is the final automated gate after an authorised PR/push. A green CI result does not replace required manual Android testing.

If the work grows into a higher Validation Level, report it before continuing. Stop for a Human decision if the change also crosses the approved scope or a stop rule.

## Cost control

Use a fresh agent conversation for a new Micro Job when possible.

Keep context small:

- prefer targeted file reads and path-scoped diffs;
- do not reread large unchanged files;
- summarise successful test/build results;
- on failure, inspect the failed step and first useful error before opening more logs;
- rerun the failed/focused check first, then finish only the validation required by the job.

## Git safety

Do not stage, commit, push, create a PR, or merge unless that action is explicitly authorised.

Also:

- do not use `git reset --hard`;
- do not use `git clean -fd`;
- do not force push;
- do not stage unrelated files;
- avoid `git add .` and `git add -A` unless explicitly authorised;
- never merge into `main` automatically.

## Stop and ask

Report `HUMAN DECISION REQUIRED` if the job unexpectedly needs:

- a Room schema migration;
- a new external dependency;
- a breaking public/interface change;
- a major architecture change;
- substantial code deletion;
- significant work outside the Issue;
- repair of an unrelated pre-existing failure;
- a destructive Git action.

State the reason, affected area, and practical options.

## Handoff

Keep the final report short:

- Micro Job ID and Validation Level
- changed files and purpose
- validation run and result
- CI status, if available
- manual validation still needed
- `git diff --stat`
- `git status`
- unexpected findings or escalation
