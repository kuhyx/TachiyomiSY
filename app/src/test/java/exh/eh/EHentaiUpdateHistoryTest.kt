package exh.eh

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.manga.model.FavoriteEntryAlternative
import java.util.Date

@RunWith(RobolectricTestRunner::class)
internal class EHentaiUpdateHistoryTest {
    private val harness = EhHelperHarness()
    private val current = listOf(ehChapter(11, 1, "/s/a"), ehChapter(12, 1, "/s/b"), ehChapter(13, 1, "/s/c"))
    private val chainChapters = current + listOf(ehChapter(21, 2, "/s/a"), ehChapter(22, 2, "/s/b"))
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

    @Test
    fun historyMovesToAcceptedChapters() {
        val history = listOf(
            // two reads of "/s/a" on the discarded chapter: the latest wins
            ehHistory(1, 21, readAt = 100),
            ehHistory(2, 21, readAt = 200),
            // a read of "/s/b" on the accepted chapter itself: nothing to move
            ehHistory(3, 12, readAt = 300),
            // a chapter no chain knows about
            ehHistory(4, 99, readAt = 400),
            // never read
            ehHistory(5, 22, readAt = null),
        )
        val (newHistory, toDelete) = helper.getHistory(current, chainChapters, history)
        newHistory shouldContainExactly listOf(HistoryUpdate(11, Date(200), 2))
        toDelete shouldContainExactly listOf(1L, 2L, 4L, 5L)
    }

    @Test
    fun unreadHistoryIsDropped() {
        val (newHistory, toDelete) = helper.getHistory(current, chainChapters, listOf(ehHistory(5, 22, null)))
        newHistory.shouldBeEmpty()
        toDelete shouldContainExactly listOf(5L)
    }

    @Test
    fun alternativeNeedsAFavourite() {
        val accepted = ChapterChain(ehManga(1), emptyList(), emptyList())
        helper.getFavoriteEntryAlternative(accepted, listOf(ChapterChain(ehManga(3), emptyList(), emptyList())))
            .shouldBeNull()
        val favourite = ChapterChain(ehManga(2, favorite = true), emptyList(), emptyList())
        helper.getFavoriteEntryAlternative(accepted, listOf(favourite)) shouldBe
            FavoriteEntryAlternative(otherGid = "1", otherToken = "tok1", gid = "2", token = "tok2")
    }

    @Test
    fun chapterListMergesAndCopies() {
        val kept = ehChapter(13, 1, "/s/c", dateUpload = 9)
        val accepted = ChapterChain(ehManga(1), listOf(ehChapter(11, 1, "/s/a", dateUpload = 5), kept), emptyList())
        val discarded = ChapterChain(
            ehManga(2, favorite = true),
            listOf(ehChapter(21, 2, "/s/a", 5).copy(read = true), ehChapter(22, 2, "/s/b", 1).copy(lastPageRead = 3)),
            emptyList(),
        )
        val all = accepted.chapters + discarded.chapters
        val (updates, new, hasNew) = helper.getChapterList(accepted, listOf(discarded), all)
        hasNew shouldBe true
        updates.map { it.id } shouldContainExactly listOf(11L, 13L)
        updates.first().name shouldBe "v2: c11"
        updates.first().read.shouldBeNull()
        updates.last().name shouldBe "v3: c13"
        new.single().url shouldBe "/s/b"
        new.single().name shouldBe "v1: c22"
        new.single().lastPageRead shouldBe 3
        val (_, none, nothingNew) = helper.getChapterList(accepted, emptyList(), emptyList())
        none.shouldBeEmpty()
        nothingNew shouldBe false
    }

    @Test
    fun mergeDeletesThenInserts() = runBlocking<Unit> {
        val accepted = ChapterChain(ehManga(1), current, emptyList())
        coEvery { harness.getChaptersByMangaId.await(1L) } returns current
        helper.mergeHistoryInto(accepted, chainChapters, listOf(ehHistory(1, 21, readAt = 100), ehHistory(4, 99, 400)))
        coVerify(exactly = 1) { harness.removeHistory.awaitById(1L) }
        coVerify(exactly = 1) { harness.removeHistory.awaitById(4L) }
        coVerify(exactly = 1) { harness.upsertHistory.await(HistoryUpdate(11, Date(100), 1)) }
    }
}
