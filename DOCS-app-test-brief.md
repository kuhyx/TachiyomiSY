<!-- The brief every agent working on `app` test coverage follows. Written 2026-09-25 during
     the coverage campaign; see CLAUDE.md for the repo-wide rules it sits under. -->

# Brief: unit tests for the `app` module of TachiyomiSY (Android application, Kotlin, Compose)

Repo root is the checkout you are in (other agents may write tests for other packages of the same module at
the same time -- you only ever create files under `app/src/test/java/` for YOUR packages, and
never touch `app/src/main/`, `app/build.gradle.kts`, or anything outside `app/src/test/`).
Main sources: `app/src/main/java/<package path>/<Name>.kt`.
Tests go in:  `app/src/test/java/<same package path>/<Name>Test.kt` (split: `<Name>XxxTest.kt`).
Fixtures (HTML/JSON bodies for MockWebServer): `app/src/test/resources/<package path>/*.json`.
Read each assigned source file FULLY before writing its tests.

Goal: 100% LINE and 100% BRANCH coverage (JaCoCo via Kover) of the files assigned to you.
Per-file gaps come from `scripts/kover_gaps.py` over the Kover XML report (see above). Generated code (`BuildConfig`,
`*.databinding.*`, the AIDL stub) is already excluded; everything else counts.
Every `if`/`else`, `?:`, `?.`, `when` branch, `&&`/`||` short-circuit, default argument (call
with AND without it), `require`/`check` failing path, `catch` path and lambda must execute.
A line that is genuinely unreachable without changing main code goes in your report with
file:line and why -- do not fight it in the test, and do not edit main code.

## Verify yourself (run these; do not claim anything you have not run)
```bash
# one slice, fast loop (a few packages at a time)
./gradlew :app:testDebugUnitTest --tests 'eu.kanade.tachiyomi.ui.reader.*'
# the whole module + the coverage report, when the slice is done
./gradlew :app:testDebugUnitTest :app:koverXmlReportDebug -PsyAppCoverage
python3 scripts/kover_gaps.py app/build/reports/kover/reportDebug.xml | less   # per-file gaps
# the lint stack the gate runs (detekt + ktlint through Spotless)
./gradlew :app:detekt :app:spotlessCheck
./gradlew :app:spotlessApply        # fixes formatting, including import order
# everything CI runs, before you open the PR
scripts/ci_gates.sh
```
`-PsyAppCoverage` is what applies Kover to `app`; without it there is no report.
Coverage is measured on the **debug** variant only (`koverVerifyDebug`), because that
is the only variant AGP generates unit tests for.

## Hard rules (a gate rejects violations; do not rely on me to fix them)
- Every test file <= 250 lines (split into `FooTest.kt`, `FooParseTest.kt`, ...).
- Test classes are `internal class FooTest`; top-level helpers `internal fun`/`internal val`
  (explicit API mode is on for tests too). Function names <= 30 characters, plain camelCase.
  At most 10 `@Test` per class.
- No `@Suppress`, no detekt/ktlint suppressions, no wildcard imports, no `println`.
  `@OptIn(...)` is not a suppression; the module already opts in to ExperimentalMaterial3Api,
  ExperimentalFoundationApi, ExperimentalLayoutApi, ExperimentalComposeUiApi,
  ExperimentalCoroutinesApi, FlowPreview, ExperimentalSerializationApi and more (tests inherit).
- Warnings are errors: no deprecated calls (call a `@Deprecated` member through kotlin-reflect
  if it must be covered), no unused variables/params (unused lambda params are `_`).
- ktlint `intellij_idea` style: 4-space indent, max line 120, trailing commas in multi-line
  argument lists, imports lexicographic with `java.`, `javax.`, `kotlin.` groups last.
- Detekt `NamedArguments`: any call with more than 3 arguments is fully named. `MagicNumber`
  does NOT apply to tests. `DestructuringDeclarationWithTooManyEntries`: max 3.
  `LabeledExpression` is active: never `this@Foo` / `return@label`. `MaxChainedCallsOnSameLine`:
  at most 5 chained calls per line. `FunctionMaxLength` 30 applies to tests.
- Do NOT mix JUnit 4 and JUnit 5 `@Test` in one class.

## Libraries on the test classpath
JUnit 5 (`org.junit.jupiter.api.Test`, `BeforeEach`, `AfterEach`, `io.TempDir`), JUnit 4 +
Robolectric (`org.junit.Test`, `org.junit.Rule`, `org.junit.runner.RunWith`,
`org.robolectric.RobolectricTestRunner`, `org.robolectric.annotation.Config`,
`androidx.test.core.app.ApplicationProvider`), kotest assertions (`io.kotest.matchers.shouldBe`,
`shouldNotBe`, `io.kotest.matchers.nulls.shouldBeNull`, `io.kotest.matchers.collections.
shouldContainExactly`, `io.kotest.assertions.throwables.shouldThrow`), mockk (`mockk`, `every`,
`coEvery`, `verify`, `coVerify`, `mockkStatic`, `mockkObject`, `unmockkAll`, `spyk`, `slot`),
kotlinx-coroutines-test (`runTest`, `StandardTestDispatcher`, `Dispatchers.setMain`),
kotlinx.serialization (json, protobuf), okhttp 5 + `mockwebserver3` (`MockWebServer`,
`MockResponse`, `.enqueue`, `server.url("/")`), Compose `ui-test-junit4`
(`androidx.compose.ui.test.junit4.v2.createComposeRule` -- the `v2` one, the other is
deprecated), kotlin-reflect. Turbine is NOT available.
Working Robolectric + Compose example that passes here:
`app/src/test/java/eu/kanade/presentation/components/EmptyScreenTest.kt`. For the house style of
a whole slice, read `app/src/test/java/eu/kanade/tachiyomi/data/track/` (Koin graph in
`TrackKoin.kt`, `MapPreferenceStore`, MockWebServer fixtures) and
`app/src/test/java/exh/` (Robolectric + Compose + Voyager).

## Recipes for this module
- **DI.** `Injekt.get<T>()` / `by injectLazy()` resolve through Koin's global context
  (injekt-koin fork). In a test: `startKoin { modules(module { single<T> { mockk() } }) }`
  in `@BeforeEach` (or `@Before`) and `stopKoin()` in `@AfterEach`/`@After`; an unregistered
  type throws `NoDefinitionFoundException`. Register EVERY type the code under test pulls
  (grep the file and what it calls for `Injekt.get`/`injectLazy`). Screen models take most
  collaborators as constructor defaults (`= Injekt.get()`): pass mocks explicitly AND cover the
  default (one construction with Koin providing them).
- **Preferences.** Never mock `Preference<T>`: build the real store
  `tachiyomi.core.common.preference.InMemoryPreferenceStore()` and hand it to
  `BasePreferences(context, store)`, `LibraryPreferences(store)`, `UnsortedPreferences(store)`...
- **Android.** Plain JVM tests (JUnit 5) for logic; `@RunWith(RobolectricTestRunner::class)`
  (JUnit 4) when the code touches `Context`, resources, `Uri`, `Intent`, `Bitmap`, `WebView`,
  notifications, `WorkManager`; `ApplicationProvider.getApplicationContext<Context>()` is a
  bare `android.app.Application` (the real `App` is NOT booted -- `robolectric.properties`
  replaces it). `sdk=35`. A `Context` needed only for `getString`/`getExternalFilesDir`: mockk it.
- **Compose screens.** `@get:Rule val compose = createComposeRule()`,
  `compose.setContent { MaterialTheme { ... } }` (screens read `MaterialTheme`; the app's
  `TachiyomiTheme` pulls `UiPreferences` from Injekt -- to compose it, register
  `UiPreferences(InMemoryPreferenceStore())` in Koin first); after every state change
  `compose.waitForIdle()`. Voyager screens: wrap in `Navigator(screen)` from
  `cafe.adriel.voyager.navigator`; screens that `rememberScreenModel` need Koin providing the
  model's collaborators. Find nodes by `onNodeWithText`/`onNodeWithContentDescription`;
  `useUnmergedTree = true` for children of clickables. Strings come from
  `i18n/src/commonMain/moko-resources/base/strings.xml` and `i18n-sy/.../strings.xml`
  (`MR.strings.x` / `SYMR.strings.x`); read the English text there instead of guessing.
  Every `@Composable` with default arguments: one call omitting each default, one passing it.
  `@Preview` composables are ordinary composables: compose them.
- **Network.** Sources/trackers get `NetworkHelper` from Injekt: register
  `mockk<NetworkHelper> { every { client } returns OkHttpClient() }` and point base URLs at a
  `MockWebServer` when the class allows; otherwise intercept with an `OkHttpClient.Builder()
  .addInterceptor { chain -> Response.Builder()... }` that serves a fixture from
  `src/test/resources`. `awaitSuccess()` throws `HttpException` on non-2xx; okhttp 5
  `String.toResponseBody(type)` appends `charset=utf-8`. Never a live request.
- **Coroutines.** `runTest` for suspend code; anything using `Dispatchers.Main` needs
  `Dispatchers.setMain(StandardTestDispatcher())` + `resetMain()`; `flow {}.first()` inside a
  lambda that never completes is a known JaCoCo gap -- report it.
- **Data classes / serializable.** Cover `equals`/`hashCode`/`toString`/`copy`; encode and
  decode `@Serializable` classes with full and minimal JSON.
- **Tag tables** (`exh/eh/tags/*.kt`, `TagList` objects with `getTags1()...`): one test per
  object calling every `getTagsN()` and asserting size/first/last; ~30k trivial lines.
- Traps from the other modules (all real): a mockk field initialised before `@BeforeEach` runs
  the mocked class's `<clinit>`; coroutine stack-trace recovery copies exceptions (compare the
  cause chain); `when(Boolean)` keeps a default arm JaCoCo counts; a `by lazy` is a branch
  (read it twice); `groups[n]?.value?.toDouble()!!` has an unreachable null arm (report it).

## Report back (concise)
1. The list of test files you wrote (paths) and roughly what each covers.
2. The final coverage of each main file you own, from `scripts/kover_gaps.py`
   (`lines x/y branches x/y`).
3. Any line/branch you could not reach and why (file:line).
4. Anything you had to assume about a dependency's behaviour.
