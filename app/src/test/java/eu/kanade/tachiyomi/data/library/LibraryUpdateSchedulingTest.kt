package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.net.NetworkCapabilities
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.workManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import java.util.UUID

/** Periodic scheduling, manual starts (chained after a sync when enabled) and stopping. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateSchedulingTest : LibraryUpdateTestBase() {

    private val workManager = mockk<WorkManager>(relaxed = true)
    private val syncPreferences = mockk<SyncPreferences>()
    private val periodic = slot<PeriodicWorkRequest>()
    private val manual = slot<OneTimeWorkRequest>()

    @Before
    fun setUpWorkManager() {
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
        every { workManager.isRunning(any()) } returns false
        every { syncPreferences.isSyncEnabled() } returns false
        loadKoinModules(module { single { syncPreferences } })
        every { workManager.enqueueUniquePeriodicWork(any(), any(), capture(periodic)) } returns mockk()
        every { workManager.enqueueUniqueWork(any(), any(), capture(manual)) } returns mockk()
    }

    private fun schedule(restrictions: Set<String>, interval: Int? = 12) {
        libraryPreferences.autoUpdateDeviceRestrictions.set(restrictions)
        LibraryUpdateJob.setupTask(context, interval)
    }

    @Test
    fun noIntervalCancels() {
        libraryPreferences.autoUpdateInterval.set(0)
        schedule(emptySet(), interval = null)
        verify { workManager.cancelUniqueWork(LibraryUpdateJob.WORK_NAME_AUTO) }
    }

    @Test
    fun restrictionsBecomeConstraints() {
        schedule(setOf(DEVICE_ONLY_ON_WIFI, DEVICE_NETWORK_NOT_METERED, DEVICE_CHARGING))
        val strict = periodic.captured.workSpec.constraints
        strict.requiredNetworkRequest!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) shouldBe true
        strict.requiredNetworkRequest!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) shouldBe true
        strict.requiresCharging() shouldBe true
        schedule(emptySet())
        val loose = periodic.captured.workSpec.constraints
        loose.requiredNetworkRequest!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) shouldBe false
        loose.requiresCharging() shouldBe false
        val policy = ExistingPeriodicWorkPolicy.UPDATE
        verify { workManager.enqueueUniquePeriodicWork(LibraryUpdateJob.WORK_NAME_AUTO, policy, any()) }
    }

    @Test
    fun manualStartEnqueues() {
        val category = tachiyomi.domain.category.model.Category(id = 3L, name = "C", order = 0L, flags = 0L)
        LibraryUpdateJob.startNow(context, category) shouldBe true
        manual.captured.workSpec.input.getLong(LibraryUpdateJob.KEY_CATEGORY, -1L) shouldBe 3L
        LibraryUpdateJob.startNow(context) shouldBe true
        val name = LibraryUpdateJob.WORK_NAME_MANUAL
        verify(exactly = 2) { workManager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>()) }
    }

    @Test
    fun runningUpdateIsNotRestarted() {
        every { workManager.isRunning(LibraryUpdateJob.TAG) } returns true
        LibraryUpdateJob.startNow(context) shouldBe false
    }

    @Test
    fun syncRunsFirst() {
        every { syncPreferences.isSyncEnabled() } returns true
        val chain = mockk<WorkContinuation>(relaxed = true)
        every { workManager.beginUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns chain
        every { chain.then(any<OneTimeWorkRequest>()) } returns chain
        LibraryUpdateJob.startNow(context) shouldBe true
        verify { chain.enqueue() }
        every { workManager.isRunning(match { it != LibraryUpdateJob.TAG }) } returns true
        LibraryUpdateJob.startNow(context) shouldBe false
    }

    @Test
    fun stopReschedulesAutoWork() {
        libraryPreferences.autoUpdateInterval.set(0)
        val (autoId, manualId) = UUID.randomUUID() to UUID.randomUUID()
        val autoRun = mockk<WorkInfo> {
            every { id } returns autoId
            every { tags } returns setOf(LibraryUpdateJob.WORK_NAME_AUTO)
        }
        val manualRun = mockk<WorkInfo> {
            every { id } returns manualId
            every { tags } returns setOf(LibraryUpdateJob.WORK_NAME_MANUAL)
        }
        every { workManager.getWorkInfos(any()) } returns immediateFuture(listOf(autoRun, manualRun))
        LibraryUpdateJob.stop(context)
        verify { workManager.cancelWorkById(autoId) }
        verify { workManager.cancelWorkById(manualId) }
        verify(exactly = 1) { workManager.cancelUniqueWork(LibraryUpdateJob.WORK_NAME_AUTO) }
    }
}
