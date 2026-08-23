# Repository Copilot Instructions

Purpose
- Provide repository-wide rules and guardrails for automated agents and contributors.
- Preserve the project's MVVM + Clean Architecture intent and testing discipline.

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
- Work on only one requested job at a time.
- Do not perform unrelated refactoring or formatting changes.
- Inspect callers and downstream dependencies before changing behaviour.
- Make the smallest correct change that satisfies the request.

Testing rules
- Add or update focused unit tests when behaviour changes.
- Run focused tests locally first (single test or package) before running the whole suite.
- Validate on the platform's Gradle wrapper:
  - On Linux/macOS and GitHub Actions: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
  - On Windows: `gradlew.bat testDebugUnitTest` and `gradlew.bat assembleDebug`.
- Run `testDebugUnitTest` and `assembleDebug` before considering implementation complete.
- Never weaken, remove, or skip tests to make a change appear to pass.

Git rules
- Check `git status` before starting work and before creating commits.
- Never modify unrelated existing changes.
- Never use `git reset --hard` or `git clean -fd` in routine workflows.
- Never force push (`git push --force`) to shared branches.
- Never merge a task branch into main automatically.
- Merging into main requires explicit human approval after review and validation.
- Do not stage, commit, push, create a PR, or merge unless explicitly requested by the active job.

Stop rules
Stop and report HUMAN DECISION REQUIRED if the job unexpectedly requires any of the following:
- Room database schema migration
- Adding a new external dependency
- Public interface changes (breaking API or exported contracts)
- Major architecture changes across layers
- Deleting substantial existing code
- Changes significantly outside the requested scope
- Fixing unrelated pre-existing failures or flaky tests

When stopping, report the following clearly:
- Reason why the stop was triggered
- Affected files and components
- Available options (with pros/cons)
- Recommended option to proceed

Developer behaviour
- Prefer the smallest correct change.
- Keep code easy to understand for a junior Android developer.
- Explain non-obvious architectural decisions in the PR or issue description.
- Do not over-engineer: prefer simple, testable solutions that fit the existing design.

Notes
- These instructions are guidance for automation agents and humans collaborating in this repository.
- They are intentionally conservative: when in doubt, stop and ask for human guidance.