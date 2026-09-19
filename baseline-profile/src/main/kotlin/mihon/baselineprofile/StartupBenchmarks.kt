package mihon.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ITERATIONS: Int = 10

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a cold state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
internal class ColdStartupBenchmark : AbstractStartupBenchmark(StartupMode.COLD)

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a warm state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
internal class WarmStartupBenchmark : AbstractStartupBenchmark(StartupMode.WARM)

/**
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance from a hot state.
 */
@RunWith(AndroidJUnit4ClassRunner::class)
internal class HotStartupBenchmark : AbstractStartupBenchmark(StartupMode.HOT)

/**
 * This test class benchmarks the speed of app startup.
 * Run this benchmark to verify how effective a Baseline Profile is.
 * It does this by comparing [CompilationMode.None], which represents the app with no Baseline
 * Profiles optimizations, and [CompilationMode.Partial], which uses Baseline Profiles.
 *
 * Run this benchmark to see startup measurements and captured system traces for verifying
 * the effectiveness of your Baseline Profiles. You can run it directly from Android
 * Studio as an instrumentation test, or run all benchmarks for a variant, for example
 * benchmarkRelease, with this Gradle task:
 * ```
 * ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * ```
 *
 * You should run the benchmarks on a physical device, not an Android emulator, because the
 * emulator doesn't represent real world performance and shares system resources with its host.
 *
 * For more information, see the Macrobenchmark documentation
 * (https://d.android.com/macrobenchmark#create-macrobenchmark) and the instrumentation arguments
 * documentation (https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args).
 **/
internal abstract class AbstractStartupBenchmark(private val startupMode: StartupMode) {
    /** Measures [ITERATIONS] launches of [TARGET_PACKAGE_NAME] per compilation mode. */
    @get:Rule
    val rule: MacrobenchmarkRule = MacrobenchmarkRule()

    /** Startup with the app interpreted only: the "no profile" baseline. */
    @Test
    fun startupCompilationNone() = benchmark(CompilationMode.None())

    /** Startup with the Baseline Profile present but not applied. */
    @Test
    fun startupProfilesDisabled() = benchmark(CompilationMode.Partial(BaselineProfileMode.Disable))

    /** Startup with the Baseline Profile applied: the number the profile is meant to improve. */
    @Test
    fun startupProfilesRequired() = benchmark(CompilationMode.Partial(BaselineProfileMode.Require))

    /** Startup with the whole app ahead-of-time compiled: the lower bound. */
    @Test
    fun startupCompilationFull() = benchmark(CompilationMode.Full())

    private fun benchmark(compilationMode: CompilationMode) {
        // The application id for the running build variant is read from the instrumentation arguments.
        rule.measureRepeated(
            packageName = TARGET_PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            startupMode = startupMode,
            iterations = ITERATIONS,
            setupBlock = {
                pressHome()
            },
            measureBlock = {
                startActivityAndWait()
            },
        )
    }
}
