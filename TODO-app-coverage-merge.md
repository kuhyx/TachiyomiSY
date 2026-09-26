# REMOVE ME AFTER FINISH

Hand-off for the session that lands the `app` coverage campaign. You have no
memory of what came before; everything you need is here, in `AGENTS.md` and in
`DOCS-app-test-brief.md`. Delete this file in the commit that finishes the job.

## Where the campaign stands (2026-09-25)

`app` is the last module without the coverage gate. Wave 1 (local agents,
merged) brought it from 226 to **45 151/87 369 lines and 5 229/21 845
branches** and added 440 test files. Nine **cloud sessions** then took a slice
each and open PRs against `kuhyx/TachiyomiSY` master. They are paid for by a
promotional cloud-session credit that expires **2026-11-04**, so their work
arrives in a burst and then stops; nothing renews it.

Slices: `ui/reader` · `ui/browse`+`ui/manga` · rest of `ui` ·
`presentation/more` · rest of `presentation` · `data/backup`+`data/sync` ·
rest of `data` · `exh/ui`+`mihon/feature`+`extension` · leftovers.
`gh pr list -R kuhyx/TachiyomiSY` is the live list (this clone's `gh` targets
`upstream` by default -- always pass `-R`).

`test-wip/` holds unfinished local drafts for the data and exh/ui slices; its
README explains them. It must be empty and deleted before the campaign ends.

## Your job, in order

1. **Review and merge the PRs.** They change main code, so read them. Reject,
   in the PR, anything that buys coverage with a suppression: a detekt
   baseline, a `@Suppress`, an entry in `.file-length-exempt`, a Kover
   `excludes` block, an allowlist entry. The only sanctioned exclusions are
   `@NativeBinding` (cannot run on a JVM) and `@InlinedOnly` (a reified inline
   stub delegating to a measured worker) -- both live in `core/common`.
2. **Adjudicate every "unreachable" claim.** Each PR body lists lines its
   author could not cover. The rule is *fix the code, do not exclude it*.
   Patterns already applied in wave 1, for reference: a `when (Boolean)`
   becomes an `if`; an unfailable `as?` cast becomes `filterIsInstance`;
   `BlockingQueue.take() != null` becomes `while (true)`; a null check on a
   value the caller guarantees becomes `!!` or `checkNotNull` with a comment;
   a string `when` becomes a map lookup (its hashCode switch has unreachable
   collision arms); a `catch` around something that cannot throw is deleted;
   Android-only code gets an injectable seam (`CrashLogUtil` takes its
   `logcat` argv) rather than an exclusion. The ONE exception class: an
   exhaustive `when` over a sealed type/enum keeps its synthetic no-match arm
   and gets a line in `app/coverage-exceptions.txt` (class, count, reason);
   PR #31 converted six such `when`s to `else` and they were reverted
   (decided 2026-09-26) -- reject that conversion wherever it reappears. A claim you cannot fix that way
   is worth a question to kuhy, not a silent exception.
3. **Expect cross-PR conflicts** on shared main-code files. Each PR body has a
   "shared main-code edits" heading listing them. Merge the smallest first,
   then ask the remaining sessions to rebase (`claude -p "<msg>" --cloud
   <session-id>` posts a message into a running cloud session).
4. **Close the gaps that are left.** The cloud sessions will not reach 100%:
   Compose screens, Voyager navigation, activities and WorkManager jobs are
   where every wave so far has stopped. Write those tests yourself, or brief
   more agents with `DOCS-app-test-brief.md`.
5. **Flip the gate** (the campaign's point). In `app/build.gradle.kts` delete
   the `if (hasProperty("syAppCoverage"))` block at the bottom and add
   `alias(mihonx.plugins.coverage)` to the `plugins` block. Then enable the
   root aggregate rule and add `koverVerify` to the Gradle gate in
   `scripts/ci_gates.sh`. `app` is already in `CAPPED_MODULES`.
6. **Prove it.** `scripts/ci_gates.sh` green locally, then green in CI, then
   one green `upstream-sync` run on GitHub.
7. **Finish the campaign's own done-condition**: build the signed drop-in APK
   (`./gradlew assembleFoss -PsyReplaceUpstream`) and have kuhy verify it on
   the phone -- the stock app must be uninstalled first, which wipes its data,
   so that is kuhy's call every time, never yours.
8. **Clean up**: delete `test-wip/`, delete this file, and update the
   `project-tachiyomisy-fork` memory to say the campaign is complete and only
   the daily sync remains.

## How to verify anything (local)

```bash
# the whole gate, as CI runs it
scripts/ci_gates.sh
# app tests + the coverage report, then the per-file gaps
JAVA_HOME=/usr/lib/jvm/java-17-openjdk CAP_MEM=8G CAP_CPU_PCT=50 \
  ~/.claude/scripts/capped.sh ./gradlew :app:testDebugUnitTest \
  :app:koverXmlReportDebug -PsyAppCoverage --max-workers=4 \
  -Dorg.gradle.jvmargs="-Xmx3g -Dfile.encoding=UTF-8"
python3 scripts/kover_gaps.py app/build/reports/kover/reportDebug.xml
```

Heavy commands go through `~/.claude/scripts/capped.sh` (2 GiB / 10% CPU by
default; `CAP_MEM=8G CAP_CPU_PCT=50` is the ceiling kuhy raised it to). Push
from the detached worktree `~/src/tachiyomisy-wt`, because the pre-push gate
reads the working tree and a dirty main clone fails it. Logs: `.logs/`.

A faster inner loop than Gradle exists at
`~/.claude/plans/tachiyomisy/tools/app_test_loop.sh` (kotlinc + JUnit console
+ the JaCoCo agent against the real app classpath, ~30 s). It needs
`app/build/test-classpath.txt`, refreshed with
`./gradlew -I ~/.claude/plans/tachiyomisy/tools/print_test_classpath.init.gradle.kts
--no-configuration-cache :app:printDebugUnitTestClasspath`. Its JaCoCo has no
Kotlin filters, so it over-reports: `@InlinedOnly` stubs and coroutine
suspension arms show as missed there but not under Kover. Kover is the
authority. `tools/ktlint_fast.sh -F` and `tools/detekt_fast.sh` lint single
files without Gradle.

## Traps that have already cost a session each

- Coverage on an application module is verified on **`koverVerifyDebug`**, not
  the total variant: AGP generates unit tests only for the test build type, and
  the total variant compiles every build type for nothing.
- Android Lint reads **test sources too** (`UnrememberedMutableState`,
  `SyntheticAccessor`), and `:app:lint` is part of the gate. A test file can
  block a push.
- Spotless enforces **import order including aliased imports**; run
  `./gradlew :app:spotlessApply` before pushing anything an agent wrote.
- `InMemoryPreferenceStore` hands out a **detached `Preference` per lookup**
  and its `changes()` completes without emitting. Tests use
  `MapPreferenceStore`/`FlowPreferenceStore` (in the test tree) instead.
- `conscrypt-android` is excluded from every unit-test runtime classpath
  (build-logic `PluginRobolectric`) because it shadows the JVM conscrypt and
  every Robolectric sandbox dies with `no conscrypt_jni`.
- An application module needs `ui-test-manifest` as `debugImplementation`, not
  just `testImplementation`, or `createComposeRule()` finds no
  `ComponentActivity`.
- `robolectric.properties` pins `sdk=35` (36+ need Java 21) and
  `application=android.app.Application`, so the real `App` never boots.
- Kotlin binds `MutableList.removeLast()` to the JDK 21 `SequencedCollection`
  method; on the JDK 17 the tests run, that is a `NoSuchMethodError`. Use
  `removeAt(lastIndex)`.
- The dependency-freshness gate goes red on its own as upstream releases; bump
  the versions, do not add an allowlist entry.

Cloud sessions see only what is committed: they have no access to `~/.claude`.
Anything a future agent must know belongs in this repo.
