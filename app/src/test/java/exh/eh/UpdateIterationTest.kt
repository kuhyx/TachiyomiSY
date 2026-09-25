package exh.eh

import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class UpdateIterationTest {
    private val harness = EhWorkerHarness()

    @Before
    fun setUp() = harness.start()

    @After
    fun tearDown() = harness.stop()

    private fun entry(manga: Manga) = UpdateEntry(manga, EHentaiSearchMetadata().apply { gId = "${manga.id}" }, null)

    private fun accept(manga: Manga, exhNew: List<Chapter> = emptyList(), root: Manga = manga) {
        coEvery { harness.updateHelper.acceptRootAndDiscardOthers(EH_SOURCE, any()) } returns
            Triple(ChapterChain(root, emptyList(), emptyList()), emptyList(), exhNew)
        coEvery { harness.getChaptersByMangaId.await(manga.id) } returns
            listOf(ehChapter(manga.id * 10, manga.id, "/s"))
    }

    @Test
    fun newChaptersCountAsUpdates() = runBlocking<Unit> {
        val manga = ehManga(1)
        val fresh = ehChapter(11, 1, "/s/new")
        harness.stubRemote(manga, listOf(fresh))
        accept(manga)
        val iteration = UpdateIteration(harness.worker(), total = 2)
        iteration.updateGallery(0, entry(manga))
        iteration.updateGallery(1, entry(manga))
        iteration.updated shouldBe 2
        iteration.failures shouldBe 0
        iteration.updatedManga.map { it.first.id } shouldContainExactly listOf(1L)
        iteration.updatedManga.single().second.toList() shouldContainExactly listOf(fresh)
        harness.libraryPreferences.newUpdatesCount.get() shouldBe 1
    }

    @Test
    fun rootChangeUsesHelperChapters() = runBlocking<Unit> {
        val iteration = UpdateIteration(harness.worker(), total = 3)
        // An unrelated gallery updated earlier in the run.
        val first = ehManga(1)
        harness.stubRemote(first, listOf(ehChapter(11, 1, "/s/first")))
        accept(first)
        iteration.updateGallery(0, entry(first))
        val manga = ehManga(2)
        val root = ehManga(3)
        val merged = ehChapter(31, 3, "/s/merged")
        harness.stubRemote(manga, listOf(ehChapter(21, 2, "/s/new")))
        accept(manga, exhNew = listOf(merged), root = root)
        iteration.updateGallery(1, entry(manga))
        iteration.updatedManga.last().first.id shouldBe 3
        iteration.updatedManga.last().second.toList() shouldContainExactly listOf(merged)
        // The root is now known: a second gallery folding into it adds nothing.
        val other = ehManga(4)
        harness.stubRemote(other, emptyList())
        accept(other, exhNew = listOf(merged), root = root)
        iteration.updateGallery(2, entry(other))
        iteration.updatedManga.size shouldBe 2
        iteration.updated shouldBe 3
    }

    @Test
    fun nothingNewAddsNothing() = runBlocking<Unit> {
        val manga = ehManga(5)
        harness.stubRemote(manga, emptyList())
        accept(manga)
        val iteration = UpdateIteration(harness.worker(), total = 1)
        iteration.updateGallery(0, entry(manga))
        iteration.updatedManga.shouldBeEmpty()
        iteration.updated shouldBe 1
    }

    @Test
    fun emptyGalleriesAreSkipped() = runBlocking<Unit> {
        val manga = ehManga(6)
        harness.stubRemote(manga, emptyList())
        val iteration = UpdateIteration(harness.worker(), total = 1)
        iteration.updateGallery(0, entry(manga))
        iteration.updated shouldBe 0
        iteration.failures shouldBe 0
    }

    @Test
    fun onlyNetworkErrorsAreFailures() = runBlocking<Unit> {
        val offline = ehManga(7)
        harness.stubRemoteFailure(offline, IllegalStateException("offline"))
        val foreign = ehManga(8, source = 99L)
        val iteration = UpdateIteration(harness.worker(), total = 2)
        iteration.updateGallery(0, entry(offline))
        iteration.updateGallery(1, entry(foreign))
        iteration.failures shouldBe 1
        iteration.updated shouldBe 0
    }

    @Test
    fun tooManyFailuresAbortTheRun() = runBlocking<Unit> {
        val galleries = (10L..17L).map { ehManga(it) }
        galleries.forEach {
            harness.stubRemoteFailure(it, IllegalStateException("offline"))
            coEvery { harness.getFlatMetadataById.await(it.id) } returns ehMetadata(it.id)
        }
        coEvery { harness.getExhFavoriteMangaWithMetadata.await() } returns galleries
        harness.worker().doWork()
        val stats = Json.decodeFromString<EHentaiUpdaterStats>(harness.exhPreferences.exhAutoUpdateStats.get())
        stats.possibleUpdates shouldBe 8
        stats.updateCount shouldBe 0
    }

    @Test
    fun updatesAreAnnounced() = runBlocking<Unit> {
        val manga = ehManga(9)
        val fresh = ehChapter(91, 9, "/s/new")
        harness.stubRemote(manga, listOf(fresh))
        accept(manga)
        coEvery { harness.getFlatMetadataById.await(9L) } returns ehMetadata(9)
        coEvery { harness.getExhFavoriteMangaWithMetadata.await() } returns listOf(manga)
        harness.worker().doWork()
        val stats = Json.decodeFromString<EHentaiUpdaterStats>(harness.exhPreferences.exhAutoUpdateStats.get())
        stats.updateCount shouldBe 1
        harness.libraryPreferences.newUpdatesCount.get() shouldBe 1
    }
}
