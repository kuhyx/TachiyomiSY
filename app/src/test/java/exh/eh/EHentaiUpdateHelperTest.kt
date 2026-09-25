package exh.eh

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.FavoriteEntryAlternative
import tachiyomi.domain.manga.model.MangaUpdate

@RunWith(RobolectricTestRunner::class)
internal class EHentaiUpdateHelperTest {
    private val harness = EhHelperHarness()

    // Manga 1 (oldest, not favourite) shares chapter "/s/a" with favourite manga 2, which also has "/s/b";
    // manga 3 shares "/s/a" but is not favourite, manga 4 is on another source and manga 5 no longer exists.
    private val acceptedChapter = ehChapter(11, 1, "/s/a", dateUpload = 1)
    private val discardedChapters = listOf(ehChapter(21, 2, "/s/a", dateUpload = 1), ehChapter(22, 2, "/s/b", 2))
    private lateinit var helper: EHentaiUpdateHelper

    @Before
    fun setUp() {
        harness.start()
        helper = harness.helper()
    }

    @After
    fun tearDown() {
        // close() cancels the runBlocking it runs in rather than the table's scope, so it always throws.
        runCatching { helper.parentLookupTable.close() }
        harness.stop()
    }

    private fun stubChains(acceptedFavorite: Boolean = false) {
        coEvery { harness.getChapterByUrl.await("/s/a") } returns
            listOf(acceptedChapter, discardedChapters[0], ehChapter(31, 3, "/s/a"), ehChapter(41, 4, "/s/a"))
        coEvery { harness.getChapterByUrl.await("/s/b") } returns listOf(discardedChapters[1], ehChapter(51, 5, "/s/b"))
        coEvery { harness.getManga.await(1L) } returns ehManga(1, favorite = acceptedFavorite)
        coEvery { harness.getManga.await(2L) } returns ehManga(2, favorite = true)
        coEvery { harness.getManga.await(3L) } returns ehManga(3)
        coEvery { harness.getManga.await(4L) } returns ehManga(4, favorite = true, source = 99L)
        coEvery { harness.getChaptersByMangaId.await(1L) } returns listOf(acceptedChapter)
        coEvery { harness.getChaptersByMangaId.await(2L) } returns discardedChapters
        coEvery { harness.getChaptersByMangaId.await(3L) } returns listOf(ehChapter(31, 3, "/s/a"))
        coEvery { harness.getCategories.await(2L) } returns listOf(Category(id = 7, name = "c", order = 0, flags = 0))
    }

    @Test
    fun singleChainIsUntouched() = runBlocking<Unit> {
        coEvery { harness.getChapterByUrl.await("/s/a") } returns listOf(acceptedChapter)
        coEvery { harness.getManga.await(1L) } returns ehManga(1)
        val (accepted, discarded, new) = helper.acceptRootAndDiscardOthers(EH_SOURCE, listOf(acceptedChapter))
        accepted.manga.id shouldBe 1
        discarded.shouldBeEmpty()
        new.shouldBeEmpty()
        coVerify(exactly = 0) { harness.updateManga.awaitAll(any()) }
    }

    @Test
    fun duplicatesFoldIntoOldest() = runBlocking<Unit> {
        stubChains()
        val fetched = listOf(acceptedChapter, discardedChapters[1])
        val (accepted, discarded, new) = helper.acceptRootAndDiscardOthers(EH_SOURCE, fetched)
        accepted.manga.id shouldBe 1
        discarded.map { it.manga.id } shouldContainExactly listOf(2)
        new.map { it.url } shouldContainExactly listOf("/s/b")
        new.single().mangaId shouldBe 1
        new.single().name shouldBe "v2: c22"
        val updates = slot<List<MangaUpdate>>()
        coVerify { harness.updateManga.awaitAll(capture(updates)) }
        updates.captured.map { it.id to it.favorite } shouldContainExactly listOf(2L to false, 1L to true)
        coVerify { harness.chapterRepository.addAll(new) }
        coVerify {
            harness.insertFavoriteEntryAlternative.await(
                FavoriteEntryAlternative(otherGid = "1", otherToken = "tok1", gid = "2", token = "tok2"),
            )
        }
        coVerify { harness.setMangaCategories.await(2L, listOf(7L)) }
        coVerify { harness.setMangaCategories.await(1L, listOf(7L)) }
    }

    @Test
    fun favouriteRootKeepsItsFlag() = runBlocking<Unit> {
        stubChains(acceptedFavorite = true)
        helper.acceptRootAndDiscardOthers(EH_SOURCE, listOf(acceptedChapter))
        val updates = slot<List<MangaUpdate>>()
        coVerify { harness.updateManga.awaitAll(capture(updates)) }
        updates.captured.map { it.id } shouldContainExactly listOf(2L)
    }

    @Test
    fun renumberNamesByUploadOrder() {
        val existing = ehChapter(1, 1, "/s/a").copy(name = "v1: c1", chapterNumber = 1.0, sourceOrder = 1)
        val fresh = ehChapter(-1, 1, "/s/b").copy(name = "v9: c2")
        val (updates, new) = helper.renumber(listOf(existing, fresh))
        updates.single().id shouldBe 1
        updates.single().name.shouldBeNull()
        updates.single().chapterNumber.shouldBeNull()
        updates.single().sourceOrder.shouldBeNull()
        new.single().name shouldBe "v2: c2"
        new.single().chapterNumber shouldBe 2.0
        new.single().sourceOrder shouldBe 0
        val (changed, _) = helper.renumber(listOf(fresh.copy(id = 5), existing))
        changed.map { it.name } shouldContainExactly listOf("v1: c2", "v2: c1")
        changed.map { it.sourceOrder } shouldContainExactly listOf(1L, 0L)
    }

    @Test
    fun galleryEntryRoundTrips() {
        val serializer = GalleryEntry.Serializer()
        val entry = GalleryEntry("123", "abc:def")
        serializer.write(entry) shouldBe "123:abc:def"
        serializer.read("123:abc:def") shouldBe entry
    }

    @Test
    fun chapterChainHoldsItsParts() {
        val chain = ChapterChain(ehManga(1), emptyList<Chapter>(), emptyList())
        chain.manga.id shouldBe 1
        chain.chapters.shouldBeEmpty()
        chain.history.shouldBeEmpty()
    }
}
