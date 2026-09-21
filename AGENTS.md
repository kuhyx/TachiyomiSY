# CLAUDE.md — TachiyomiSY (kuhy fork)

Fork of [jobobby04/TachiyomiSY](https://github.com/jobobby04/TachiyomiSY)
(Kotlin, Android, Gradle, Apache-2.0). Remote `origin` is
`github.com/kuhyx/TachiyomiSY`; `upstream` is jobobby04. Local clone:
`~/src/tachiyomisy`.

The fork exists for two things: kuhy's own fixes (backup-restore
serialization, a signed drop-in replacement build) and bringing the whole
codebase under kuhy's standing rules. The rules are not negotiable and are
enforced by gates, never by warnings:

| Rule | Gate |
|---|---|
| Every file <= 250 lines, code and prose, tests included | `scripts/check_file_length.sh` (shared, `~/src/utils`); repo-local exemptions in `.file-length-exempt`, each with a reason |
| 100% line + branch coverage, every module | Kover `koverVerify` (per module as each reaches 100%, root aggregate last) |
| Hardest lint: detekt `allRules`, ktlint, Android Lint `warningsAsErrors`, Kotlin `allWarningsAsErrors` + `-Xexplicit-api=strict` | `./gradlew check` |
| Every dependency on newest stable, exact-pinned | `scripts/check_dependency_freshness.sh` (needs the Gradle parser, see plan) |
| Markdown = README / CLAUDE* / DOCS* / TODO*; GitHub community files exempt | `scripts/check_md_naming.sh` |
| No binaries outside `.binary-allowlist` | `scripts/check_no_binaries.sh` |
| Always on top of upstream | `.github/workflows/upstream-sync.yml`, daily |

`scripts/ci_gates.sh` runs all of them; it is what CI, the pre-push hook and
the sync job execute. Add a gate there, never in a workflow alone.

## Commands

- Hooks (once per clone): `scripts/install_hooks.sh`
- All gates, as CI runs them: `scripts/ci_gates.sh` (`--no-gradle` for the
  shell gates only, seconds instead of minutes)
- Per-commit gate, detached: `scripts/ci_gates.sh --changed-only > .logs/<name>.log 2>&1 &`
  -- the pre-push hook runs the same command, so once the detached run is
  green every Gradle task is up to date and the push costs seconds. Android
  Lint is part of it since 2026-09-21 (`:app:lintAnalyzeDebug` is ~3 min,
  serial, and re-runs whenever any app file changes; the first app-lint push
  went red on findings a `-x lint` local gate had skipped). CI's `check`
  lints the debug variant -- `lintFoss` is not what CI runs.
- One module: `./gradlew :domain:check`; the convention plugins themselves:
  `./gradlew -p gradle/build-logic check` (root `check` depends on it)
- Lint stack per module: apply `mihonx.plugins.lint` (detekt every rule from
  `config/detekt/*.yml`, Android Lint warnings-as-errors, Kotlin
  `-Werror -Xexplicit-api=strict`) and `mihonx.plugins.coverage` (Kover on
  the JaCoCo engine, whose Kotlin filters drop compiler-generated dead
  branches; 100% line + branch, bound to `check`) in the commit that makes
  the module clean. Libraries do not re-lint their dependencies
  (`checkDependencies` is the app's job, last in the order). The
  compiler's explicit-API diagnostics are applied mechanically by
  `scripts/explicit_api.py <gradle-log>` (then `explicit_api_types.py` for the
  derivable return types; the rest is manual), detekt's `ExpressionBodySyntax`
  by `scripts/expression_body.py <detekt-log>`, and the consumer fallout of a
  member moved to an extension by `scripts/import_extensions.py <gradle-log>`.
  Each has a `_test.py` next to it (`python3 -m pytest scripts`).
- Drop-in APK: `./gradlew assembleFoss -PsyReplaceUpstream`
  (reads `~/.android/release/key.properties`; output
  `app/build/outputs/apk/foss/app-universal-foss.apk`)
- Upstream sync by hand: `scripts/upstream_sync.sh --dry-run`
- Phone: `phone-deploy` skill, `adb install -r`, device `23181JEGR08034`.
  The stock TachiyomiSY must be uninstalled once first (signature differs);
  that wipes its data, so it is the user's call every time.

## Upstream sync mechanics

Daily at 04:17 UTC the workflow fetches `upstream/master`, pushes it verbatim
to the fork branch `upstream` (pristine mirror: `git diff upstream master` is
always "what did we change"), rebases `master` onto it, runs every gate, and
force-pushes only on green. A conflict or a red gate opens or updates one
issue labelled `upstream-sync`; the next green run closes it. Resolution is a
manual session; an automated Claude resolver is deferred until two manual
syncs have succeeded.

Because the whole codebase is restructured (see below), expect upstream
commits to the split files to conflict. That is the accepted cost.

## Rollout order (per module, smallest first)

Each module gets its full gate set switched on in the same commit that makes
it clean; nothing runs warn-only. `gradle/build-logic` (convention plugins,
where the shared lint config lives), `source-api`, `core-metadata`,
`core/common`, `domain`, `data`, `presentation-core`, `presentation-widget`,
`source-local`, `i18n`, `i18n-sy`, `baseline-profile`, `app`. The enabled
list is `CAPPED_MODULES` in `scripts/ci_gates.sh`. Session prompts live in
`~/.claude/plans/tachiyomisy/`.

Split rules for the 250 cap: screen models -> one class per concern composed
by the original; Compose screens -> one file per composable group;
sources/scrapers -> parser, request builder, models; settings screens -> one
file per preference group. Public names stay identical so upstream diffs land
on the same symbols.

## Do not

- Touch upstream's workflows (`build_check.yml`, `build_push*.yml`,
  `issue_moderator.yml`, `pr_label.yml`); they are inert here and rebasing
  over edits to them is pure conflict.
- Add a mutating pre-commit hook (whitespace fixers, formatters on the whole
  tree): it rewrites upstream files and turns every sync into a conflict.
- Add a suppression (`@Suppress`, detekt baseline, `# noqa`, allowlist entry)
  without asking. Fix the code.
- Open pull requests against jobobby04. Work on `master` here only.
