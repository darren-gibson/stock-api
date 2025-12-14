# Contributing

Thanks for contributing! A few guidelines to keep the project maintainable.

Code style
- Project uses Spotless with ktlint. Run `./gradlew spotlessApply` before committing.

Commits
- Use conventional commits (e.g., `feat:`, `fix:`, `chore:`) where sensible.
- Keep commits small and focused; tests should pass locally.

Branches
- Create a branch per feature: `feature/<name>`
- Rebase or squash feature branches before merging to `master` for a tidy history.

PRs
- Open a PR against `master`, add a clear description, and ensure CI passes.

Tests
- Add unit tests for new logic and Cucumber feature scenarios for behavior.
- Run tests locally: `./gradlew test`

Code review
- Ensure at least one approving review before merging.
