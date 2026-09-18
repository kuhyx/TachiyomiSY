package tachiyomi.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.view.LibraryView
import java.util.Date

internal class LibraryQueryTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private var categoryId = 0L

    @BeforeEach
    fun seed() = runTest {
        harness.install()
        // A favourite on a normal source: one visible chapter (read, bookmarked, in history) and one from an
        // excluded scanlator, in one category.
        val alpha = database.seedManga(
            title = "Alpha",
            genre = listOf("action", "drama"),
            thumbnailUrl = "thumb",
            description = "desc",
        )
        val readChapter = database.seedChapter(mangaId = alpha, name = "1", read = true, bookmark = true)
        database.seedChapter(mangaId = alpha, name = "2", scanlator = "Bad", dateFetch = 20L, dateUpload = 50L)
        database.seedExcluded(alpha, "Bad")
        database.seedHistory(readChapter, readAt = Date(1000L))
        categoryId = database.seedCategory("Reading", order = 1L)
        database.seedMangaCategory(alpha, categoryId)
        // A favourite merged manga whose chapters live on a non-favourite child.
        val merged = database.seedManga(title = "Merged", source = MERGED_SOURCE_ID)
        val child = database.seedManga(title = "Child", source = 2L, favorite = false)
        database.seedMerged(mergeId = merged, mangaId = child)
        database.seedChapter(mangaId = child, name = "1", scanlator = "Sub", dateFetch = 11L, dateUpload = 7L)
        database.seedChapter(mangaId = child, name = "2", bookmark = true)
    }

    @AfterEach
    fun uninstall() = harness.uninstall()

    private suspend fun rows(query: LibraryQuery = getLibraryQuery()): List<LibraryView> =
        query.awaitAsList().sortedBy { it.title }

    @Test
    fun defaultConditionFavourites() = runTest {
        rows().map { it.title } shouldBe listOf("Alpha", "Merged")
    }

    @Test
    fun normalMangaAggregates() = runTest {
        val alpha = rows().first()
        alpha.source shouldBe 1L
        alpha.url shouldBe "/m/Alpha"
        alpha.artist shouldBe null
        alpha.author shouldBe null
        alpha.description shouldBe "desc"
        alpha.genre shouldBe listOf("action", "drama")
        alpha.status shouldBe 0L
        alpha.thumbnail_url shouldBe "thumb"
        alpha.favorite shouldBe true
        alpha.last_update shouldBe null
        alpha.next_update shouldBe null
        alpha.initialized shouldBe true
        alpha.viewer shouldBe 0L
        alpha.chapter_flags shouldBe 0L
        alpha.cover_last_modified shouldBe 0L
        alpha.date_added shouldBe 0L
        alpha.filtered_scanlators shouldBe null
        alpha.update_strategy shouldBe UpdateStrategy.ALWAYS_UPDATE
        alpha.calculate_interval shouldBe 0L
        alpha.favorite_modified_at shouldBe null
        alpha.is_syncing shouldBe 0L
        alpha.notes shouldBe ""
        alpha.memo shouldBe noMemo
        alpha.totalCount shouldBe 1L
        alpha.readCount shouldBe 1.0
        alpha.latestUpload shouldBe 5L
        alpha.chapterFetchedAt shouldBe 10L
        alpha.lastRead shouldBe 1000L
        alpha.bookmarkCount shouldBe 1.0
        alpha.categories shouldBe categoryId.toString()
    }

    @Test
    fun mergedMangaAggregates() = runTest {
        val merged = rows().last()
        merged.source shouldBe MERGED_SOURCE_ID
        merged.genre shouldBe null
        merged.totalCount shouldBe 2L
        merged.readCount shouldBe 0.0
        merged.latestUpload shouldBe 7L
        merged.chapterFetchedAt shouldBe 11L
        merged.lastRead shouldBe 0L
        merged.bookmarkCount shouldBe 1.0
        merged.categories shouldBe "0"
    }

    @Test
    fun customConditionSelectsRows() = runTest {
        val child = rows(getLibraryQuery(condition = "M.favorite = 0")).single()
        child.title shouldBe "Child"
        child.favorite shouldBe false
        child.totalCount shouldBe 2L
    }

    @Test
    fun queryDescribesItself() {
        val query = LibraryQuery(harness.driver)
        query.driver shouldBe harness.driver
        query.condition shouldBe "M.favorite = 1"
        query.toString() shouldBe "LibraryQuery.sq:get"
        LibraryQuery(harness.driver, condition = "1 = 1").condition shouldBe "1 = 1"
    }
}
