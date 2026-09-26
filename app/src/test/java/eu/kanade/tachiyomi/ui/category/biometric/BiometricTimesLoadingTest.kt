package eu.kanade.tachiyomi.ui.category.biometric

import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import eu.kanade.tachiyomi.ui.base.silentStore
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

/** The model before its first load: nothing to conflict with, nothing to delete, no dialogs. */
internal class BiometricTimesLoadingTest {
    private val prefs = SecurityPreferences(silentStore())
    private val range = TimeRange(1.hours, 2.hours)

    @BeforeEach
    fun setUp() {
        mainUnconfined()
    }

    @AfterEach
    fun tearDown() = mainReset()

    @Test
    fun dialogsAreIgnored() {
        val model = BiometricTimesScreenModel(prefs)
        model.showDialog(BiometricTimesDialog.Create)
        model.dismissDialog()
        model.state.value shouldBe BiometricTimesScreenState.Loading
    }

    @Test
    fun createAddsWithoutChecking() {
        val model = BiometricTimesScreenModel(prefs)
        model.createTimeRange(range)
        waitFor { prefs.authenticatorTimeRanges.get() == setOf(range.toPreferenceString()) }
    }

    @Test
    fun deleteDoesNothing() {
        prefs.authenticatorTimeRanges.set(setOf(range.toPreferenceString()))
        val model = BiometricTimesScreenModel(prefs)
        model.deleteTimeRanges(TimeRangeItem(range, "r"))
        // Whenever the delete runs, it finds no loaded state and writes nothing, so both ranges stay.
        model.createTimeRange(TimeRange(3.hours, 4.hours))
        waitFor { prefs.authenticatorTimeRanges.get().size == 2 }
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition never held" }
            Thread.sleep(10)
        }
    }
}
