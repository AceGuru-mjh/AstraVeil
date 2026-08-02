## Summary

<!-- Brief description of what this PR changes and why. -->

## Related Issue

Closes #
<!-- Or: Refs # -->

## Change Type

- [ ] feat — new feature
- [ ] fix — bug fix
- [ ] docs — documentation only
- [ ] refactor — code change that neither fixes a bug nor adds a feature
- [ ] test — adding or correcting tests
- [ ] ci — CI/CD changes
- [ ] build — build system / dependencies
- [ ] perf — performance improvement

## Affected Modules

- [ ] `:app`
- [ ] `:core`
- [ ] `:providers`
- [ ] `:sdk`
- [ ] `:modules`
- [ ] `:native`
- [ ] `:proto`
- [ ] `daemon/`
- [ ] `rust/`
- [ ] `astrahub/`
- [ ] docs / CI / tooling

## Checklist

- [ ] Unit tests pass (`./gradlew testDebugUnitTest`)
- [ ] detekt passes (`./gradlew detekt`)
- [ ] No hardcoded dependency strings (use `gradle/libs.versions.toml`)
- [ ] New public API has KDoc / Doxygen documentation
- [ ] IPC protocol changes updated `proto/` definitions
- [ ] Security-relevant changes updated `docs/THREAT_MODEL.md`
- [ ] AstraHub index changes pass `python3 tools/validate-hub.py --all --strict`
- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)
- [ ] `CHANGELOG.md` updated (for user-facing changes)

## Notes for Reviewers

<!-- Anything reviewers should pay special attention to, tricky edge cases,
     breaking changes, or testing instructions. -->
