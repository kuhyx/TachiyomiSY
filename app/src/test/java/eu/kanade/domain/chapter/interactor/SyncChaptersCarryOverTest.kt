package eu.kanade.domain.chapter.interactor

import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.toChapterUpdate
import java.time.ZonedDateTime

internal class SyncChaptersCarryOverTest {

    private val harness = SyncChaptersHarness()
    private val interactor = harness.interactor

    @Test
    fun stampsDescendingFetchDates() {
        val diff = SyncChaptersWithSource.Diff(
            new = mutableListOf(dbChapter("/b", chapterNumber = 2.0), dbChapter("/a", chapterNumber = 1.0)),
            removed = emptyList(),
        )
        val changed = mutableSetOf<String>()
        val result = interactor.carryOverRemovedState(diff, emptyList(), nowMillis = 1000, changed)
        result.map { it.dateFetch } shouldBe listOf(1002L, 1001L)
        result.none { it.read } shouldBe true
        changed shouldBe emptySet()
    }

    @Test
    fun marksDuplicateReadsRead() {
        harness.libraryPreferences.markDuplicateReadChapterAsRead.set(setOf("new"))
        val db = listOf(
            dbChapter("/old", chapterNumber = 1.0, read = true),
            dbChapter("/unread", chapterNumber = 2.0),
            dbChapter("/unnumbered", chapterNumber = -1.0, read = true),
        )
        val diff = SyncChaptersWithSource.Diff(
            new = mutableListOf(dbChapter("/dup", chapterNumber = 1.0), dbChapter("/new", chapterNumber = 2.0)),
            removed = emptyList(),
        )
        val changed = mutableSetOf<String>()
        val result = interactor.carryOverRemovedState(diff, db, nowMillis = 0, changed)
        result.map { it.read } shouldBe listOf(true, false)
        changed shouldBe setOf("/dup")
    }

    @Test
    fun ignoresDuplicatesByDefault() {
        val db = listOf(dbChapter("/old", chapterNumber = 1.0, read = true))
        val diff = SyncChaptersWithSource.Diff(
            new = mutableListOf(dbChapter("/dup", chapterNumber = 1.0)),
            removed = emptyList(),
        )
        val changed = mutableSetOf<String>()
        interactor.carryOverRemovedState(diff, db, nowMillis = 0, changed).single().read shouldBe false
        changed shouldBe emptySet()
    }

    @Test
    fun inheritsRemovedState() {
        val removedRead = dbChapter("/r1", chapterNumber = 1.0, read = true).copy(dateFetch = 500)
        val removedMarked = dbChapter("/r2", chapterNumber = 2.0, bookmark = true).copy(dateFetch = 600)
        val removedPlain = dbChapter("/r3", chapterNumber = 3.0).copy(dateFetch = 700)
        val diff = SyncChaptersWithSource.Diff(
            new = mutableListOf(
                dbChapter("/n1", chapterNumber = 1.0),
                dbChapter("/n2", chapterNumber = 2.0),
                dbChapter("/n3", chapterNumber = 3.0),
                dbChapter("/n4", chapterNumber = 4.0),
                dbChapter("/n5", chapterNumber = -1.0),
            ),
            removed = listOf(removedRead, removedMarked, removedPlain),
        )
        val changed = mutableSetOf<String>()
        val result = interactor.carryOverRemovedState(diff, emptyList(), nowMillis = 1000, changed)
        result.map { it.read } shouldBe listOf(true, false, false, false, false)
        result.map { it.bookmark } shouldBe listOf(false, true, false, false, false)
        result.map { it.dateFetch } shouldBe listOf(500L, 600L, 700L, 1002L, 1001L)
        changed shouldBe setOf("/n1", "/n2", "/n3")
    }

    @Test
    fun ehProgressCarriesOverIfApplies() {
        val eh = libraryManga(source = EH_SOURCE_ID)
        val progressed = listOf(dbChapter("/v1").copy(lastPageRead = 12))
        val toAdd = listOf(dbChapter("/v2"), dbChapter("/seen"))
        interactor.carryOverEhProgress(libraryManga(), progressed, toAdd, emptySet()) shouldBe toAdd
        interactor.carryOverEhProgress(eh, emptyList(), toAdd, emptySet()) shouldBe toAdd
        interactor.carryOverEhProgress(eh, listOf(dbChapter("/v1")), toAdd, emptySet()) shouldBe toAdd
        interactor.carryOverEhProgress(eh, progressed, toAdd, setOf("/v2", "/seen")) shouldBe toAdd
        interactor.carryOverEhProgress(eh, progressed, emptyList(), emptySet()) shouldBe emptyList()
        val result = interactor.carryOverEhProgress(eh, progressed, toAdd, setOf("/seen"))
        result.map { it.lastPageRead } shouldBe listOf(12L, 0L)
        val several = listOf(3L, 12L, 7L).map { dbChapter("/p$it").copy(lastPageRead = it) }
        interactor.carryOverEhProgress(eh, several, toAdd, emptySet()).map { it.lastPageRead } shouldBe listOf(12L, 12L)
        val exh = libraryManga(source = EXH_SOURCE_ID)
        interactor.carryOverEhProgress(exh, progressed, toAdd, emptySet()).all { it.lastPageRead == 12L } shouldBe true
    }

    @Test
    fun persistWritesOnlyWhatChanged() = runTest {
        harness.stubWrites()
        val manga = libraryManga()
        val now = ZonedDateTime.now()
        val empty = SyncChaptersWithSource.Diff(removed = emptyList())
        interactor.persist(empty, emptyList(), manga, now, 0L to 0L) shouldBe emptyList()
        coVerify(exactly = 0) { harness.chapterRepository.removeChaptersWithIds(any()) }
        coVerify(exactly = 0) { harness.chapterRepository.addAll(any()) }
        coVerify(exactly = 0) { harness.updateChapter.awaitAll(any()) }
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateFetchInterval(manga, now, 0L to 0L) }
        coVerify(exactly = 1) { harness.updateManga.awaitUpdateLastUpdate(1) }

        val removed = dbChapter("/gone")
        val updated = dbChapter("/changed")
        val diff = SyncChaptersWithSource.Diff(
            new = mutableListOf(),
            updated = mutableListOf(updated),
            removed = listOf(removed),
        )
        val toAdd = listOf(dbChapter("/new"))
        coEvery { harness.chapterRepository.addAll(toAdd) } returns toAdd.map { it.copy(id = 42) }
        interactor.persist(diff, toAdd, manga, now, 0L to 0L).single().id shouldBe 42L
        coVerify(exactly = 1) { harness.chapterRepository.removeChaptersWithIds(listOf(removed.id)) }
        coVerify(exactly = 1) { harness.updateChapter.awaitAll(listOf(updated.toChapterUpdate())) }
    }
}
