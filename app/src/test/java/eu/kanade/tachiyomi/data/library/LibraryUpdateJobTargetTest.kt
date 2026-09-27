package eu.kanade.tachiyomi.data.library

import androidx.work.ListenableWorker.Result
import androidx.work.workDataOf
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob.Target
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class LibraryUpdateJobTargetTest {

    private val harness = LibraryJobHarness()

    @Before
    fun setUp() {
        harness.start()
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { any<MdUtil>().getEnabledMangaDex(any<SourcePreferences>(), any<SourceManager>()) } returns null
    }

    @After
    fun tearDown() = harness.stop()

    private suspend fun work(target: Target?) =
        harness.job(workDataOf(LibraryUpdateJob.KEY_TARGET to target?.name)).doWork()

    @Test
    fun chaptersStampTheUpdate() = runTest {
        work(null) shouldBe Result.success()
        (harness.libraryPreferences.lastUpdatedTimestamp.get() > 0L) shouldBe true
    }

    @Test
    fun coversKeepTheStamp() = runTest {
        work(Target.COVERS) shouldBe Result.success()
        harness.libraryPreferences.lastUpdatedTimestamp.get() shouldBe 0L
    }

    @Test
    fun followsNeedAMangaDex() = runTest {
        work(Target.SYNC_FOLLOWS) shouldBe Result.success()
    }

    @Test
    fun pushWithoutLoginIsANoOp() = runTest {
        coEvery { harness.getFavorites.await() } returns emptyList()
        work(Target.PUSH_FAVORITES) shouldBe Result.success()
    }

    @Test
    fun failureIsReported() = runTest {
        coEvery { harness.getFavorites.await() } throws IllegalStateException("db gone")
        work(Target.PUSH_FAVORITES) shouldBe Result.failure()
        harness.logged.any { it.contains("db gone") } shouldBe true
    }

    @Test
    fun cancellationCountsAsSuccess() = runTest {
        coEvery { harness.getFavorites.await() } throws CancellationException("stop")
        work(Target.PUSH_FAVORITES) shouldBe Result.success()
    }

    @Test
    fun skippedEntriesAreLogged() = runTest {
        val once = libManga(1, title = "Once").copy(updateStrategy = UpdateStrategy.ONLY_FETCH_ONCE)
        coEvery { harness.getLibraryManga.await() } returns listOf(libraryEntry(once, total = 3))
        work(Target.COVERS) shouldBe Result.success()
        harness.logged.any { it == "Skipped because series does not require updates: [Once]" } shouldBe true
    }
}
