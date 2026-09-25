package eu.kanade.tachiyomi.ui.category.biometric

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import eu.kanade.tachiyomi.ui.base.oneShotStore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import kotlin.time.Duration.Companion.hours

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
internal class BiometricTimesScreenModelTest {
    private val prefs = SecurityPreferences(MapPreferenceStore())
    private val morning = TimeRange(8.hours, 12.hours)

    @Before
    fun setUp() {
        mainUnconfined()
        val app = ApplicationProvider.getApplicationContext<Application>()
        startKoin {
            modules(
                module {
                    single { app }
                    single { prefs }
                },
            )
        }
        prefs.authenticatorTimeRanges.set(setOf(morning.toPreferenceString(), "broken"))
    }

    @After
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun BiometricTimesScreenModel.success(size: Int = 1): BiometricTimesScreenState.Success =
        state.await { it is BiometricTimesScreenState.Success && it.timeRanges.size == size }.shouldBeInstanceOf()

    @Test
    fun rangesAreFormatted() {
        val state = BiometricTimesScreenModel().success()
        state.timeRanges shouldBe listOf(TimeRangeItem(morning, "8:00 AM - 12:00 PM"))
        state.isEmpty shouldBe false
        state.copy(timeRanges = emptyList()).isEmpty shouldBe true
    }

    @Test
    fun conflictingRangeIsRejected() {
        val model = BiometricTimesScreenModel(prefs)
        model.success()
        model.createTimeRange(TimeRange(9.hours, 13.hours))
        model.events.await { true } shouldBe BiometricTimesEvent.TimeConflicts
    }

    @Test
    fun freeRangeIsAdded() {
        val model = BiometricTimesScreenModel(prefs)
        model.success()
        model.createTimeRange(TimeRange(13.hours, 14.hours))
        model.success(size = 2).timeRanges.last().timeRange shouldBe TimeRange(13.hours, 14.hours)
    }

    @Test
    fun rangeIsDeleted() {
        val model = BiometricTimesScreenModel(prefs)
        val item = model.success().timeRanges.single()
        model.deleteTimeRanges(item)
        model.success(size = 0).isEmpty shouldBe true
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = BiometricTimesScreenModel(prefs)
        val item = model.success().timeRanges.single()
        model.showDialog(BiometricTimesDialog.Create)
        model.success().dialog shouldBe BiometricTimesDialog.Create
        model.showDialog(BiometricTimesDialog.Delete(item))
        model.success().dialog shouldBe BiometricTimesDialog.Delete(item)
        model.dismissDialog()
        model.success().dialog shouldBe null
    }

    @Test
    fun finishedSourceKeepsTheState() {
        val oneShot = SecurityPreferences(oneShotStore())
        oneShot.authenticatorTimeRanges.set(setOf(morning.toPreferenceString()))
        BiometricTimesScreenModel(oneShot).success().timeRanges.single().timeRange shouldBe morning
    }

    @Test
    fun eventMembers() {
        BiometricTimesEvent.InternalError.stringRes shouldBe MR.strings.internal_error
        BiometricTimesEvent.TimeConflicts.stringRes shouldBe SYMR.strings.biometric_lock_time_conflicts
        val delete = BiometricTimesDialog.Delete(TimeRangeItem(morning, "m"))
        delete.copy().hashCode() shouldBe delete.hashCode()
    }
}
