package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.workDataOf
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.saver.withSdk
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.isRunning
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD

/** The worker: when an automatic run may start, which pass it runs, and how a pass ends. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateJobTest : LibraryUpdateTestBase() {

    private val workManager = mockk<WorkManager>()

    @Before
    fun setUpWorker() {
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        coEvery { any<CoroutineWorker>().setForegroundSafely() } returns Unit
        every { any<Context>().workManager } returns workManager
        every { workManager.isRunning(LibraryUpdateJob.WORK_NAME_MANUAL) } returns false
        mockkStatic("eu.kanade.tachiyomi.util.system.NetworkExtensionsKt")
        every { any<Context>().isConnectedToWifi() } returns false
        coEvery { getLibraryManga.await() } returns emptyList()
    }

    private fun target(target: LibraryUpdateJob.Target) = workDataOf(LibraryUpdateJob.KEY_TARGET to target.name)

    private fun auto(): LibraryUpdateJob = job(tags = setOf(LibraryUpdateJob.WORK_NAME_AUTO))

    @Test
    fun chapterRunStampsTheTime() = runTest {
        job().doWork() shouldBe ListenableWorker.Result.success()
        (libraryPreferences.lastUpdatedTimestamp.get() > 0L) shouldBe true
    }

    @Test
    fun manualRunBlocksAutoRun() = runTest {
        every { workManager.isRunning(LibraryUpdateJob.WORK_NAME_MANUAL) } returns true
        auto().doWork() shouldBe ListenableWorker.Result.retry()
        every { workManager.isRunning(LibraryUpdateJob.WORK_NAME_MANUAL) } returns false
        auto().doWork() shouldBe ListenableWorker.Result.success()
    }

    @Test
    fun oldAndroidHonoursWifiOnly() {
        val results = withSdk(Build.VERSION_CODES.O) {
            runBlocking {
                libraryPreferences.autoUpdateDeviceRestrictions.set(emptySet())
                val free = auto().doWork()
                libraryPreferences.autoUpdateDeviceRestrictions.set(setOf(DEVICE_ONLY_ON_WIFI))
                val offWifi = auto().doWork()
                every { any<Context>().isConnectedToWifi() } returns true
                listOf(free, offWifi, auto().doWork())
            }
        }
        val (success, retry) = ListenableWorker.Result.success() to ListenableWorker.Result.retry()
        results shouldBe listOf(success, retry, success)
    }

    @Test
    fun coverRunKeepsTheTime() = runTest {
        job(target(LibraryUpdateJob.Target.COVERS)).doWork() shouldBe ListenableWorker.Result.success()
        libraryPreferences.lastUpdatedTimestamp.get() shouldBe 0L
    }

    @Test
    fun mangaDexTargetsRun() = runTest {
        coEvery { getFavorites.await() } returns emptyList()
        job(target(LibraryUpdateJob.Target.PUSH_FAVORITES)).doWork() shouldBe ListenableWorker.Result.success()
        coVerify { getFavorites.await() }
        loadKoinModules(module { single { SourcePreferences(store) } })
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } returns null
        job(target(LibraryUpdateJob.Target.SYNC_FOLLOWS)).doWork() shouldBe ListenableWorker.Result.success()
    }

    @Test
    fun failuresAndCancellation() = runTest {
        val entry = libraryManga(manga(1L))
        coEvery { getLibraryManga.await() } returns listOf(entry)
        libraryPreferences.autoUpdateMangaRestrictions.set(emptySet())
        coEvery { getManga.await(1L) } throws IllegalStateException("db")
        job().doWork() shouldBe ListenableWorker.Result.failure()
        coEvery { getManga.await(1L) } throws CancellationException("stopped")
        job().doWork() shouldBe ListenableWorker.Result.success()
    }

    @Test
    fun skippedEntriesAreLogged() = runTest {
        libraryPreferences.autoUpdateMangaRestrictions.set(setOf(MANGA_HAS_UNREAD))
        val first = libraryManga(manga(2L, title = "B"), total = 2L)
        coEvery { getLibraryManga.await() } returns listOf(first, libraryManga(manga(1L, title = "A"), total = 1L))
        job().doWork()
        logged.joinToString() shouldContain "Skipped because there are unread chapters: [A, B]"
    }

    @Test
    fun categoryInputSelects() = runTest {
        coEvery { getLibraryManga.await() } returns listOf(libraryManga(manga(1L), categories = listOf(4L)))
        libraryPreferences.autoUpdateMangaRestrictions.set(emptySet())
        coEvery { getManga.await(1L) } returns null
        job(workDataOf(LibraryUpdateJob.KEY_CATEGORY to 4L)).doWork() shouldBe ListenableWorker.Result.success()
        coVerify { getManga.await(1L) }
    }

    @Test
    fun foregroundTypeFollowsSdk() = runTest {
        job().getForegroundInfo().foregroundServiceType shouldBe ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        withSdk(Build.VERSION_CODES.P) { runBlocking { job().getForegroundInfo() } }.foregroundServiceType shouldBe 0
    }
}
