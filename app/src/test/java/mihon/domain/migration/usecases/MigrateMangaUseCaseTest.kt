package mihon.domain.migration.usecases

import eu.kanade.domain.track.model.domainTrack
import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.domain.migration.usecases.MigrateMangaHarness.Companion.CURRENT_SOURCE
import mihon.domain.migration.usecases.MigrateMangaHarness.Companion.TARGET_SOURCE
import mihon.domain.migration.usecases.MigrateMangaHarness.Companion.current
import mihon.domain.migration.usecases.MigrateMangaHarness.Companion.target
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import java.io.File
import kotlin.io.path.createTempFile

internal class MigrateMangaUseCaseTest {

    private val harness = MigrateMangaHarness()
    private val useCase = harness.useCase

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { harness.coverCache } }) }
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt")
        every { harness.downloadManager.deleteManga(any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    @Test
    fun unknownTargetSourceDoesNothing() = runTest {
        every { harness.sourceManager.get(TARGET_SOURCE) } returns null
        useCase(current, target, replace = true)
        coVerify(exactly = 0) { harness.updateMangaFromRemote(any<Manga>(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun remoteFailureAbortsQuietly() = runTest {
        harness.stubHappyPath()
        coEvery { harness.updateMangaFromRemote(target, fetchChapters = true, throttleFunc = any()) } returns
            Result.failure(IllegalStateException("offline"))
        useCase(current, target, replace = true)
        coVerify(exactly = 0) { harness.updateManga.awaitAll(any()) }
    }

    @Test
    fun cancellationPropagates() = runTest {
        harness.stubHappyPath()
        coEvery { harness.updateMangaFromRemote(target, fetchChapters = true, throttleFunc = any()) } throws
            CancellationException("stop")
        var cancelled = false
        try {
            useCase(current, target, replace = true)
        } catch (expected: CancellationException) {
            cancelled = expected.message == "stop"
        }
        cancelled shouldBe true
    }

    @Test
    fun noFlagsOnlySwapsFavourites() = runTest {
        harness.stubHappyPath()
        harness.sourcePreferences.migrationFlags.set(emptySet())
        useCase(current, target, replace = false)
        coVerify(exactly = 0) { harness.updateChapter.awaitAll(any()) }
        coVerify(exactly = 0) { harness.setMangaCategories.await(any(), any()) }
        verify(exactly = 0) { harness.downloadManager.deleteManga(any(), any(), any()) }
        coVerify(exactly = 0) { harness.insertTrack.awaitAll(any()) }
        val updates = harness.mangaUpdates.single()
        updates.size shouldBe 1
        updates[0].id shouldBe 20L
        updates[0].favorite shouldBe true
        updates[0].chapterFlags shouldBe 5L
        updates[0].viewerFlags shouldBe 9L
        updates[0].notes shouldBe null
        (updates[0].dateAdded!! > 1_000L) shouldBe true
    }

    @Test
    fun everyFlagWithReplace() = runTest {
        harness.stubHappyPath()
        val cover = createTempFile("cover").toFile()
        every { harness.coverCache.getCustomCoverFile(10) } returns cover
        every { harness.coverCache.setCustomCoverToCache(target, any()) } returns Unit
        coEvery { harness.getCategories.await(10) } returns listOf(Category(id = 3, name = "c", order = 0, flags = 0))
        useCase(current, target, replace = true) { }
        coVerify(exactly = 1) { harness.setMangaCategories.await(20, listOf(3L)) }
        verify(exactly = 1) { harness.downloadManager.deleteManga(current, harness.currentSource, true) }
        verify(exactly = 1) { harness.coverCache.setCustomCoverToCache(target, any()) }
        val updates = harness.mangaUpdates.single()
        updates.map { it.id } shouldBe listOf(10L, 20L)
        updates[0].favorite shouldBe false
        updates[0].dateAdded shouldBe 0L
        updates[1].dateAdded shouldBe 1_000L
        updates[1].notes shouldBe "keep me"
    }

    @Test
    fun missingCurrentSourceAndCover() = runTest {
        harness.stubHappyPath()
        every { harness.sourceManager.get(CURRENT_SOURCE) } returns null
        every { harness.coverCache.getCustomCoverFile(10) } returns File("missing-cover")
        useCase(current, target, replace = true)
        verify(exactly = 0) { harness.downloadManager.deleteManga(any(), any(), any()) }
        verify(exactly = 0) { harness.coverCache.setCustomCoverToCache(any(), any()) }
        harness.mangaUpdates.single().size shouldBe 2
    }

    @Test
    fun tracksMoveToTheTarget() = runTest {
        harness.stubHappyPath()
        harness.sourcePreferences.migrationFlags.set(emptySet())
        val enhanced = mockk<BaseTracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
        every { harness.trackerManager.trackers } returns listOf(mockk(), enhanced)
        val plain = domainTrack(id = 1, trackerId = 1)
        val owned = domainTrack(id = 2, trackerId = 2)
        val dropped = domainTrack(id = 3, trackerId = 2)
        coEvery { harness.getTracks.await(10) } returns listOf(plain, owned, dropped)
        val tracker = enhanced as EnhancedTracker
        val source = harness.currentSource
        every { tracker.isTrackFrom(plain.copy(mangaId = 20), current, source) } returns false
        every { tracker.isTrackFrom(owned.copy(mangaId = 20), current, source) } returns true
        every { tracker.isTrackFrom(dropped.copy(mangaId = 20), current, source) } returns true
        every { tracker.migrateTrack(owned.copy(mangaId = 20), target, harness.targetSource) } returns
            owned.copy(mangaId = 20, remoteId = 99)
        every { tracker.migrateTrack(dropped.copy(mangaId = 20), target, harness.targetSource) } returns null
        useCase(current, target, replace = false)
        harness.trackInserts.single() shouldBe listOf(
            plain.copy(mangaId = 20),
            owned.copy(mangaId = 20, remoteId = 99),
        )
    }
}
