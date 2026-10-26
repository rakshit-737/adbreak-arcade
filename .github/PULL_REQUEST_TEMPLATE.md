## Summary

<!-- What changed, and why? Keep this focused on the user-visible result. -->

## Change type

- [ ] Feature
- [ ] Bug fix
- [ ] Refactor
- [ ] Tests
- [ ] Documentation
- [ ] Build or CI

## Scope

- Affected areas:
- User-visible behavior:
- Persistence or migration impact:
- Permissions or platform behavior affected:

## Verification

List the exact commands you ran and their results.

```text
# Example:
.\gradlew.bat testDebugUnitTest lintDebug
```

- [ ] Unit tests pass.
- [ ] Lint passes.
- [ ] Website checks pass, if website files changed.
- [ ] `git diff --check` passes.
- [ ] Device/emulator testing status is described accurately.

## Review checklist

- [ ] I read the affected callers, resources, and tests before changing them.
- [ ] I did not commit secrets, signing keys, local configuration, or generated output.
- [ ] I preserved the existing Kotlin/Android Views/XML architecture unless this PR explains a necessary change.
- [ ] I kept game rules separate from notification rendering and Android UI.
- [ ] I added or updated tests for new behavior and important edge cases.
- [ ] I updated user-facing documentation where behavior or limitations changed.
- [ ] This change does not claim automatic ad detection, ad skipping, ad blocking, forced notification-shade opening, or guaranteed media continuity.
