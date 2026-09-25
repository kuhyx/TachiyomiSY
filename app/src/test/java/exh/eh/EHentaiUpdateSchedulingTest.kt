package exh.eh

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.Printer
import exh.source.ExhPreferences
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING

@RunWith(RobolectricTestRunner::class)
internal class EHentaiUpdateSchedulingTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val preferences = ExhPreferences(InMemoryPreferenceStore())

    @Before
    fun setUp() {
        XLog.init(LogConfiguration.Builder().build(), Printer { _, _, _ -> })
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun backgroundTestIsExpedited() {
        EHentaiUpdateWorker.launchBackgroundTest(context)
        val request = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(request)) }
        request.captured.tags shouldContain EHentaiUpdateWorker.TAG
        request.captured.workSpec.expedited.shouldBeTrue()
    }

    @Test
    fun scheduleReadsPreferences() {
        preferences.exhAutoUpdateFrequency.set(6)
        preferences.exhAutoUpdateRequirements.set(setOf(DEVICE_CHARGING))
        EHentaiUpdateWorker.scheduleBackground(context)
        val request = slot<PeriodicWorkRequest>()
        verify {
            workManager.enqueueUniquePeriodicWork(
                EHentaiUpdateWorker.TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(request),
            )
        }
        request.captured.workSpec.intervalDuration shouldBe 6 * 60 * 60 * 1000L
        request.captured.workSpec.constraints.requiresCharging().shouldBeTrue()
        request.captured.workSpec.constraints.requiredNetworkType shouldBe NetworkType.CONNECTED
    }

    @Test
    fun scheduleHonoursArguments() {
        preferences.exhAutoUpdateFrequency.set(0)
        EHentaiUpdateWorker.scheduleBackground(context, prefInterval = 2, prefRestrictions = emptySet())
        val request = slot<PeriodicWorkRequest>()
        verify { workManager.enqueueUniquePeriodicWork(any(), any(), capture(request)) }
        request.captured.workSpec.intervalDuration shouldBe 2 * 60 * 60 * 1000L
        request.captured.workSpec.constraints.requiresCharging().shouldBeFalse()
    }

    @Test
    fun zeroIntervalCancels() {
        preferences.exhAutoUpdateFrequency.set(0)
        EHentaiUpdateWorker.scheduleBackground(context)
        verify(exactly = 1) { workManager.cancelAllWorkByTag(EHentaiUpdateWorker.TAG) }
        verify(exactly = 0) { workManager.enqueueUniquePeriodicWork(any(), any(), any<PeriodicWorkRequest>()) }
        EHentaiUpdateWorker.cancelBackground(context)
        verify(exactly = 2) { workManager.cancelAllWorkByTag(EHentaiUpdateWorker.TAG) }
    }
}
