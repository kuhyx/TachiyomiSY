package tachiyomi.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.view.UpdatesView

internal class UpdatesQueryTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private var mergedId = 0L
    private var childId = 0L
    private var mergedChapterId = 0L

    @BeforeEach
    fun seed() = runTest {
        harness.install()
        // A favourite with an unread, an excluded-scanlator, a read+bookmarked and a started chapter.
        val alpha = database.seedManga(title = "Alpha")
        database.seedChapter(mangaId = alpha, name = "a1", dateFetch = 10L, dateUpload = 5L)
        database.seedChapter(mangaId = alpha, name = "a2", scanlator = "Bad", dateFetch = 20L, dateUpload = 6L)
        database.seedExcluded(alpha, "Bad")
        database.seedChapter(
            mangaId = alpha,
            name = "a3",
            read = true,
            bookmark = true,
            dateFetch = 30L,
            dateUpload = 7L,
        )
        database.seedChapter(mangaId = alpha, name = "a4", lastPageRead = 2L, dateFetch = 40L, dateUpload = 8L)
        // A favourite merged manga whose chapter lives on a non-favourite child.
        mergedId = database.seedManga(title = "Merged", source = MERGED_SOURCE_ID)
        childId = database.seedManga(title = "Child", source = 2L, favorite = false)
        database.seedMerged(mergeId = mergedId, mangaId = childId)
        mergedChapterId = database.seedChapter(
            mangaId = childId,
            name = "c1",
            scanlator = "Sub",
            dateFetch = 50L,
            dateUpload = 9L,
        )
        // Not favourite, and a favourite whose chapter was fetched before the manga was added: never listed.
        val skipped = database.seedManga(title = "Skipped", favorite = false)
        database.seedChapter(mangaId = skipped, name = "s1", dateFetch = 60L)
        val old = database.seedManga(title = "Old", dateAdded = 100L)
        database.seedChapter(mangaId = old, name = "o1", dateFetch = 70L)
    }

    @AfterEach
    fun uninstall() = harness.uninstall()

    private suspend fun names(filter: UpdatesFilter = ALL): List<String> =
        getUpdatesQuery(filter).awaitAsList().map { it.chapterName }

    @Test
    fun listsNewestFetchFirst() = runTest {
        names() shouldBe listOf("c1", "a4", "a3", "a1")
    }

    @Test
    fun excludedScanlatorsCanBeShown() = runTest {
        val rows = getUpdatesQuery(ALL.copy(hideExcludedScanlators = 0L)).awaitAsList()
        rows.map { it.chapterName } shouldBe listOf("c1", "a4", "a3", "a2", "a1")
        rows.map { it.excludedScanlator } shouldBe listOf(null, null, null, "Bad", null)
    }

    @Test
    fun readFilter() = runTest {
        names(ALL.copy(read = true)) shouldBe listOf("a3")
        names(ALL.copy(read = false)) shouldBe listOf("c1", "a4", "a1")
    }

    @Test
    fun startedFilter() = runTest {
        names(ALL.copy(started = 1L)) shouldBe listOf("a4")
        names(ALL.copy(started = 0L)) shouldBe listOf("c1", "a1")
    }

    @Test
    fun bookmarkedFilter() = runTest {
        names(ALL.copy(bookmarked = true)) shouldBe listOf("a3")
        names(ALL.copy(bookmarked = false)) shouldBe listOf("c1", "a4", "a1")
    }

    @Test
    fun afterAndLimit() = runTest {
        names(ALL.copy(after = 7L)) shouldBe listOf("c1", "a4")
        names(ALL.copy(limit = 2L)) shouldBe listOf("c1", "a4")
    }

    @Test
    fun mapsEveryColumn() = runTest {
        val row = getUpdatesQuery(ALL).awaitAsList().first()
        row shouldBe UpdatesView(
            mangaId = mergedId,
            mangaTitle = "Merged",
            chapterId = mergedChapterId,
            chapterName = "c1",
            scanlator = "Sub",
            chapterUrl = "/c/$childId/c1",
            read = false,
            bookmark = false,
            last_page_read = 0L,
            source = MERGED_SOURCE_ID,
            favorite = true,
            thumbnailUrl = null,
            coverLastModified = 0L,
            dateUpload = 9L,
            datefetch = 50L,
            excludedScanlator = null,
        )
    }

    @Test
    fun queryDescribesItself() {
        val query = UpdatesQuery(harness.driver, ALL)
        query.driver shouldBe harness.driver
        query.filter shouldBe ALL
        query.toString() shouldBe "LibraryQuery.sq:get"
    }

    private companion object {
        val ALL = UpdatesFilter(
            after = 0L,
            limit = 10L,
            read = null,
            started = null,
            bookmarked = null,
            hideExcludedScanlators = 1L,
        )
    }
}
