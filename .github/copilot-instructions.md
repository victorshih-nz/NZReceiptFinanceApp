# Repository Copilot Instructions

Purpose
- Provide repository-wide rules and guardrails for automated agents and contributors.
- Preserve the project's MVVM + Clean Architecture intent and risk-based validation discipline.
- Minimise unnecessary AI context, repeated validation, and compute without weakening required quality gates.

Before starting work (required checks)
- Run `git status` and confirm the working tree is clean.
- Confirm the current branch matches the explicitly authorised task branch.
- Never modify repository files directly on `main`.
- If currently on `main` and the task requires repository modifications, stop and report HUMAN DECISION REQUIRED.
- Do not modify production or test code unless the task explicitly requests it.

Architecture rules
- Preserve MVVM: UI logic must remain in presentation (Fragments, ViewModels), not moved into domain or data.
- Preserve Clean Architecture boundaries:
  - presentation may depend on domain
  - data may depend on domain
  - domain must not depend on presentation or data
  - domain must remain independent of Android UI, Room, and framework implementation details
  - DI/composition root wires concrete implementations to domain interfaces
- Respect responsibilities: presentation (UI, adapters, state), domain (models, use cases, interfaces, business rules), data (Room, OCR, parsers, repositories), DI (AppContainer, ViewModelFactory).
- Prefer using or extending existing architecture before adding new abstractions or layers.
- Do not move responsibilities between layers without clear justification and documented rationale.

Scope rules
- Work on only one requested Micro Job at a time.
- Do not split, combine, redesign, or expand a Micro Job unless explicitly authorised.
- Do not perform unrelated refactoring or formatting changes.
- Inspect relevant callers and downstream dependencies before changing behaviour.
- Make the smallest correct change that satisfies the active Micro Job.

Validation rules
- Every implementation Micro Job must have an assigned Validation Level: L1, L2, L3, or L4.
- Follow `docs/agent/VALIDATION_LEVEL_COST_DOWN_POLICY.md` for detailed level definitions and cost-control rules.
- The active GitHub Issue is the source of truth for the assigned level and job-specific validation.
- Use only the validation required by that level and active Micro Job; do not mechanically run the full Android test/build sequence for every job.
- If actual work requires a higher level, report the escalation before substantial extra work.
- If escalation crosses scope, architecture, dependency, Room migration, destructive operation, or another Stop rule, stop and report HUMAN DECISION REQUIRED.
- Always run `git diff --check` before reporting a Micro Job ready for Git action/review.
- Add or update focused unit tests when behaviour changes and focused unit testing is applicable.
- Never weaken, remove, bypass, or skip relevant tests to make a change appear to pass.
- GitHub Actions is the authoritative final automated validation environment after an authorised push/PR.
- GitHub Actions independently decides whether Android Gradle validation is required; Agent A must not try to bypass that remote gate.
- A successful GitHub Actions gate does not replace required Human manual Android validation.

Validation levels
- L1 — Documentation / non-runtime configuration:
  - Run lightweight relevant checks such as `git diff --check` and syntax/schema validation.
  - Do not run local `testDebugUnitTest` or `assembleDebug` by default.
- L2 — Logic / unit-testable code:
  - Run relevant focused unit tests.
  - Run local `testDebugUnitTest` only when shared-risk impact, failure evidence, or the active Issue requires it.
  - Do not run local `assembleDebug` by default when Android resources, Manifest, Navigation, DI, or build wiring are unchanged.
- L3 — Android integration / UI / DI:
  - Run relevant focused tests when applicable.
  - Run local `assembleDebug`.
  - Run local `testDebugUnitTest` only when the active Issue or cross-component risk requires it.
  - Human manual Android validation is required for user-visible/device-dependent behaviour.
- L4 — Persistence / build / cross-layer high risk:
  - For runtime/persistence work, normally run focused tests, local `testDebugUnitTest`, and local `assembleDebug`.
  - For GitHub Actions workflow-only work, use workflow-specific validation and an actual GitHub Actions run; local Android unit/build tasks are not automatically required solely to validate YAML/control flow.
  - Human manual validation is required when persistence, user-visible data, device behaviour, or end-to-end flow is affected.

Gradle commands
- On Linux/macOS and GitHub Actions use `./gradlew`.
- On Windows use `gradlew.bat`.
- Run only the Gradle tasks required by the assigned Validation Level and active Micro Job.

AI context and compute rules
- Use one fresh Agent conversation for each new Micro Job when separate conversations are supported.
- Treat repository instructions/approved design documents as long-term project memory and the active GitHub Issue as the current job contract.
- Read the active Issue, referenced design sections, expected components, and relevant direct callers/dependencies first.
- Do not scan or reread the entire repository by default.
- Expand inspection only when a relevant dependency, failing test, architecture uncertainty, or Stop-rule risk requires it.
- Do not repeatedly reread unchanged large files/design documents within the same Micro Job.
- For successful tests/builds/CI, use concise result summaries; do not copy or analyse full successful logs.
- For failures, inspect the failed step and first relevant error with limited surrounding context, then expand only if needed.
- Prefer concise Git commands such as `git status --short`, `git diff --stat`, and path-scoped `git diff` when sufficient.
- Do not repeatedly rerun expensive tests after every small edit. Re-run the relevant failed/focused check first, then complete the validation required by the assigned level once the focused check passes.

CI failure classification
- Classify a CI failure before attempting a fix:
  - Job-introduced
  - Pre-existing
  - CI Infrastructure
  - Flaky or uncertain
- Fix only Job-introduced failures within the active Micro Job.
- Do not repair unrelated pre-existing or flaky failures unless explicitly authorised.

Git rules
- Check `git status` before starting work and before creating commits.
- Never modify unrelated existing changes.
- Never use `git reset --hard` or `git clean -fd` in routine workflows.
- Never force push (`git push --force`) to shared branches.
- Never merge a task branch into main automatically.
- Merging into main requires explicit human approval after review and validation.
- Do not stage, commit, push, create a PR, or merge unless explicitly requested by the active job.
- Stage only explicitly authorised paths; do not use broad staging such as `git add .` or `git add -A` unless explicitly authorised.

Stop rules
Stop and report HUMAN DECISION REQUIRED if the job unexpectedly requires any of the following:
- Room database schema migration
- Adding a new external dependency
- Public interface changes (breaking API or exported contracts)
- Major architecture changes across layers
- Deleting substantial existing code
- Changes significantly outside the requested scope
- Fixing unrelated pre-existing failures or flaky tests
- Destructive Git operations

When stopping, report:
- reason why the stop was triggered;
- affected files/components;
- available options with pros/cons;
- recommended option.

Developer behaviour
- Prefer the smallest correct change.
- Keep code easy to understand for a junior Android developer.
- Explain non-obvious architectural decisions in the PR or Issue.
- Do not over-engineer: prefer simple, testable solutions that fit the approved design.
- Do not invent missing product behaviour. If an acceptance criterion or interaction is materially ambiguous, request a Human/SA decision.

Completion and handoff
- After local implementation/validation, report readiness for the next authorised Git action.
- Do not claim final Micro Job PASS before required GitHub Actions and Agent B final review are complete.
- Keep reports concise.
- Report:
  - Micro Job ID;
  - Validation Level;
  - changed files and purpose;
  - local validation executed and PASS/FAIL/NOT REQUIRED;
  - GitHub Actions status when available;
  - manual validation requirement;
  - `git diff --stat`;
  - `git status`;
  - unexpected findings/validation escalation;
  - Stop-rule status.
- Do not include full successful test/build logs.

Notes
- Validation is risk-based.
- The goal is minimum unnecessary compute, not minimum engineering assurance.
- When in doubt about scope, architecture, persistence, dependencies, or destructive actions, stop and ask for Human guidance.
