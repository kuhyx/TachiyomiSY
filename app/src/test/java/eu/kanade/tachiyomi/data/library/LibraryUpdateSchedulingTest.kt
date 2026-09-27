package eu.kanade.tachiyomi.data.library

import android.net.NetworkCapabilities
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob.Target
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateSchedulingTest {

    private val harness = LibraryJobHarness()
    private val context get() = harness.context
    private val wm get() = harness.workManager

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun scheduled(): PeriodicWorkRequest {
        val request = slot<PeriodicWorkRequest>()
        verify {
            wm.enqueueUniquePeriodicWork(
                LibraryUpdateJob.WORK_NAME_AUTO,
                ExistingPeriodicWorkPolicy.UPDATE,
                capture(request),
            )
        }
        return request.captured
    }

    @Test
    fun restrictionsShapeConstraints() {
        harness.libraryPreferences.autoUpdateDeviceRestrictions.set(
            setOf(DEVICE_NETWORK_NOT_METERED, DEVICE_ONLY_ON_WIFI, DEVICE_CHARGING),
        )
        LibraryUpdateJob.setupTask(context, prefInterval = 12)
        val constraints = scheduled().workSpec.constraints
        val network = constraints.requiredNetworkRequest
        network?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) shouldBe true
        network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) shouldBe true
        constraints.requiresCharging() shouldBe true
    }

    @Test
    fun openNetworkIsEnough() {
        harness.libraryPreferences.autoUpdateDeviceRestrictions.set(emptySet())
        harness.libraryPreferences.autoUpdateInterval.set(24)
        LibraryUpdateJob.setupTask(context)
        val request = scheduled()
        val network = request.workSpec.constraints.requiredNetworkRequest
        network?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) shouldBe false
        network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) shouldBe false
        request.workSpec.constraints.requiresCharging() shouldBe false
    }

    @Test
    fun zeroIntervalCancels() {
        LibraryUpdateJob.setupTask(context)
        verify { wm.cancelUniqueWork(LibraryUpdateJob.WORK_NAME_AUTO) }
    }

    @Test
    fun runningUpdateBlocksStart() {
        harness.running(tag = LibraryUpdateJob.TAG, state = WorkInfo.State.RUNNING)
        LibraryUpdateJob.startNow(context) shouldBe false
    }

    @Test
    fun runningSyncBlocksStart() {
        harness.syncPreferences.syncService.set(1)
        harness.running(tag = LibraryJobHarness.SYNC_JOB_TAG, state = WorkInfo.State.RUNNING)
        LibraryUpdateJob.startNow(context) shouldBe false
    }

    @Test
    fun syncRunsFirst() {
        harness.syncPreferences.syncService.set(1)
        LibraryUpdateJob.startNow(context, category = Category(id = 3, name = "c", order = 0, flags = 0)) shouldBe true
        verify {
            wm.beginUniqueWork(LibraryUpdateJob.WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>())
        }
    }

    @Test
    fun plainStartEnqueues() {
        val request = slot<OneTimeWorkRequest>()
        LibraryUpdateJob.startNow(context, target = Target.COVERS, group = 2, groupExtra = "x") shouldBe true
        verify { wm.enqueueUniqueWork(LibraryUpdateJob.WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, capture(request)) }
        val input = request.captured.workSpec.input
        input.getString(LibraryUpdateJob.KEY_TARGET) shouldBe "COVERS"
        input.getString(LibraryUpdateJob.KEY_GROUP_EXTRA) shouldBe "x"
    }

    @Test
    fun stopReschedulesAuto() {
        harness.libraryPreferences.autoUpdateInterval.set(0)
        val auto = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf(LibraryUpdateJob.WORK_NAME_AUTO))
        val manual = WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, setOf(LibraryUpdateJob.WORK_NAME_MANUAL))
        every { wm.getWorkInfos(any()) } returns immediateFuture(listOf(auto, manual))
        LibraryUpdateJob.stop(context)
        verify { wm.cancelWorkById(auto.id) }
        verify { wm.cancelWorkById(manual.id) }
        verify(exactly = 1) { wm.cancelUniqueWork(LibraryUpdateJob.WORK_NAME_AUTO) }
    }
}
