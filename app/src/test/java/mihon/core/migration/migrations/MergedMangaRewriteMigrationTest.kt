package mihon.core.migration.migrations

import app.cash.sqldelight.Query
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.source.Source
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.EhQueries
import tachiyomi.data.awaitList
import tachiyomi.domain.chapter.interactor.DeleteChapters
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMangaBySource
import tachiyomi.domain.manga.interactor.InsertMergedReference
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.source.service.SourceManager

internal class MergedMangaRewriteMigrationTest {

    private val migration = MergedMangaRewriteMigration()
    private val queries = mockk<EhQueries>()
    private val database = mockk<Database> { every { ehQueries } returns queries }
    private val getMangaBySource = mockk<GetMangaBySource>()
    private val getManga = mockk<GetManga>()
    private val updateManga = mockk<UpdateManga>()
    private val insertMergedReference = mockk<InsertMergedReference>()
    private val sourceManager = mockk<SourceManager>()
    private val deleteChapters = mockk<DeleteChapters>()
    private val updateChapter = mockk<UpdateChapter>()

    @AfterEach
    fun tearDown() {
        stopMigrationKoin()
        unmockkAll()
    }

    @Test
    fun hasVersion() {
        migration.version shouldBe 7f
    }

    @Test
    fun failsWithoutCollaborators() = runTest {
        startMigrationKoin { single { database } }
        migration(migrationContext()) shouldBe false
    }

    @Test
    fun nothingToDoWithoutConfigs() = runTest {
        coEvery { getMangaBySource.await(MERGED_SOURCE_ID) } returns listOf(sourceManga(1, MERGED_SOURCE_ID, "plain"))
        startAll()
        migration(migrationContext()) shouldBe true
        coVerify(exactly = 0) { updateManga.awaitAll(any()) }
    }

    @Test
    fun rewritesReferencesAndProgress() = runTest {
        val merged = listOf(
            mergedManga(id = 10, children = listOf(100L to "/m1", 100L to "/m1", 200L to "/m2", 400L to "/gone")),
            sourceManga(id = 11, source = MERGED_SOURCE_ID, url = "garbage"),
            mergedManga(id = 12, children = emptyList()),
            mergedManga(id = 13, children = listOf(300L to "/taken")),
        )
        val m1 = sourceManga(id = 20, source = 100, url = "/m1")
        val m2 = sourceManga(id = 22, source = 200, url = "/m2")
        val source100 = mockk<Source> { every { id } returns 100L }
        val source200 = mockk<Source> { every { id } returns 200L }
        coEvery { getMangaBySource.await(MERGED_SOURCE_ID) } returns merged
        coEvery { getManga.await("/m1", MERGED_SOURCE_ID) } returns null
        coEvery { getManga.await("/taken", MERGED_SOURCE_ID) } returns sourceManga(30, MERGED_SOURCE_ID, "/taken")
        coEvery { getManga.await("/m1", 100) } returns m1
        coEvery { getManga.await("/m2", 200) } returns m2
        coEvery { getManga.await("/taken", 300) } returns null
        coEvery { getManga.await("/gone", 400) } returns null
        every { sourceManager.getOrStub(100) } returns source100
        every { sourceManager.getOrStub(200) } returns source200
        val mangaUpdates = slot<List<MangaUpdate>>()
        coEvery { updateManga.awaitAll(capture(mangaUpdates)) } returns true
        val references = slot<List<MergedMangaReference>>()
        coEvery { insertMergedReference.awaitAll(capture(references)) } returns Unit
        mockkStatic("tachiyomi.data.QueryExtensionKt")
        stubChapters(mangaIds = listOf(10, 11, 12, 13), chapters = legacyChapters())
        val mergedChapters = listOf(
            legacyChapter(id = 20, read = false, url = "/c1"),
            legacyChapter(id = 21, read = false, url = "/c2"),
        )
        stubChapters(mangaIds = listOf(20, 22), chapters = mergedChapters)
        coEvery { deleteChapters.await(listOf(20, 21)) } returns Unit
        val chapterUpdates = slot<List<ChapterUpdate>>()
        coEvery { updateChapter.awaitAll(capture(chapterUpdates)) } returns Unit
        startAll()

        migration(migrationContext()) shouldBe true

        mangaUpdates.captured shouldBe listOf(MangaUpdate(id = 10, url = "/m1"))
        references.captured.map { Triple(it.mergeId, it.mangaId, it.isInfoManga) } shouldBe listOf(
            Triple(10L, 10L, false),
            Triple(10L, 20L, true),
            Triple(10L, 22L, false),
            Triple(12L, 12L, false),
        )
        references.captured[1].mangaSourceId shouldBe 100L
        references.captured[2].mangaSourceId shouldBe 200L
        references.captured[3].mangaUrl shouldBe merged[2].url
        chapterUpdates.captured shouldBe listOf(ChapterUpdate(id = 20, read = true, lastPageRead = 0))
    }

    // One chapter per way of (not) carrying progress over; only the first one lands.
    private fun legacyChapters(): List<Chapter> = listOf(
        legacyChapter(id = 1, read = true, url = legacyChapterUrl(100, "/c1", "/m1")),
        legacyChapter(id = 2, read = false, lastPageRead = 5, url = "garbage"),
        legacyChapter(id = 3, read = false, url = legacyChapterUrl(100, "/c1", "/m1")),
        legacyChapter(id = 4, read = true, url = legacyChapterUrl(100, "/zzz", "/m1")),
        legacyChapter(id = 5, read = true, url = legacyChapterUrl(999, "/c1", "/m1")),
        legacyChapter(id = 6, read = true, url = legacyChapterUrl(100, "/c1", "/other")),
    )

    private fun stubChapters(mangaIds: List<Long>, chapters: List<Chapter>) {
        val query = mockk<Query<Chapters>>()
        every { queries.getChaptersByMangaIds(mangaIds) } returns query
        coEvery { query.awaitList<Chapters, Chapter>(any()) } returns chapters
    }

    private fun startAll() {
        startMigrationKoin {
            single { database }
            single { getMangaBySource }
            single { getManga }
            single { updateManga }
            single { insertMergedReference }
            single { sourceManager }
            single { deleteChapters }
            single { updateChapter }
        }
    }
}
