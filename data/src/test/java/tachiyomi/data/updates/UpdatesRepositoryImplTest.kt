package tachiyomi.data.updates

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.data.seedChapter
import tachiyomi.data.seedExcluded
import tachiyomi.data.seedManga
import tachiyomi.data.seedMerged

internal class UpdatesRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = UpdatesRepositoryImpl(database)
    private var mergedId = 0L

    @BeforeEach
    fun seed() = runTest {
        harness.install()
        // A favourite with an unread, a read+bookmarked, a started and an excluded-scanlator chapter.
        val alpha = database.seedManga(title = "Alpha")
        database.seedChapter(mangaId = alpha, name = "a1", dateFetch = 10L, dateUpload = 5L)
        database.seedChapter(mangaId = alpha, name = "a2", read = true, bookmark = true, dateFetch = 30L)
        database.seedChapter(mangaId = alpha, name = "a3", lastPageRead = 2L, dateFetch = 40L)
        database.seedChapter(mangaId = alpha, name = "a4", scanlator = "Bad", dateFetch = 20L)
        database.seedExcluded(alpha, "Bad")
        // A favourite merged manga whose chapter lives on a non-favourite child: only the merged-aware query sees it.
        mergedId = database.seedManga(title = "Merged", source = MERGED_SOURCE_ID, thumbnailUrl = "thumb")
        val child = database.seedManga(title = "Child", source = 2L, favorite = false)
        database.seedMerged(mergeId = mergedId, mangaId = child)
        database.seedChapter(mangaId = child, name = "c1", dateFetch = 50L, dateUpload = 9L)
    }

    @AfterEach
    fun uninstall() = harness.uninstall()

    private suspend fun names(
        unread: Boolean?,
        started: Boolean?,
        bookmarked: Boolean?,
        hideExcludedScanlators: Boolean = true,
    ): List<String> = repository.subscribeAll(
        after = 0L,
        limit = 10L,
        unread = unread,
        started = started,
        bookmarked = bookmarked,
        hideExcludedScanlators = hideExcludedScanlators,
    ).first().map { it.chapterName }

    @Test
    fun awaitWithReadUsesTheView() = runTest {
        val unread = repository.awaitWithRead(read = false, after = 0L, limit = 10L)
        unread.map { it.chapterName } shouldBe listOf("a3", "a4", "a1")
        repository.awaitWithRead(read = true, after = 0L, limit = 10L).map { it.chapterName } shouldBe listOf("a2")
        repository.awaitWithRead(read = false, after = 4L, limit = 1L).map { it.chapterName } shouldBe listOf("a3")
    }

    @Test
    fun subscribeWithReadUsesTheView() = runTest {
        val read = repository.subscribeWithRead(read = true, after = 0L, limit = 10L).first()
        read.map { it.chapterName } shouldBe listOf("a2")
        read.single().coverData.isMangaFavorite shouldBe true
    }

    @Test
    fun subscribeAllWithoutFilters() = runTest {
        names(unread = null, started = null, bookmarked = null) shouldBe listOf("c1", "a3", "a2", "a1")
    }

    @Test
    fun subscribeAllShowsExcluded() = runTest {
        names(unread = true, started = null, bookmarked = null, hideExcludedScanlators = false) shouldBe
            listOf("c1", "a3", "a4", "a1")
    }

    @Test
    fun subscribeAllReadBookmarked() = runTest {
        names(unread = false, started = null, bookmarked = true) shouldBe listOf("a2")
    }

    @Test
    fun subscribeAllStarted() = runTest {
        names(unread = true, started = true, bookmarked = false) shouldBe listOf("a3")
        names(unread = null, started = false, bookmarked = null) shouldBe listOf("c1", "a1")
    }

    @Test
    fun subscribeAllMapsMergedRows() = runTest {
        val row = repository.subscribeAll(
            after = 0L,
            limit = 1L,
            unread = null,
            started = null,
            bookmarked = null,
            hideExcludedScanlators = true,
        ).first().single()
        row.mangaId shouldBe mergedId
        row.mangaTitle shouldBe "Merged"
        row.sourceId shouldBe MERGED_SOURCE_ID
        row.dateFetch shouldBe 50L
        row.coverData.url shouldBe "thumb"
    }
}
