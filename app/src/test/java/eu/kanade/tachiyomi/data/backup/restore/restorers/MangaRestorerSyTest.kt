package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.backupFlatMetadata
import eu.kanade.tachiyomi.data.backup.models.backupMergedMangaReference
import eu.kanade.tachiyomi.data.sync.mangasRow
import exh.metadata.metadata.base.FlatMetadata
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Merged
import tachiyomi.domain.manga.model.CustomMangaInfo

internal class MangaRestorerSyTest {

    private val harness = MangaRestorerHarness()
    private val reference = backupMergedMangaReference()

    @BeforeEach
    fun setUp() = harness.start()

    @AfterEach
    fun tearDown() = harness.stop()

    private fun stored(mergeUrl: String, mangaUrl: String) = Merged(
        _id = 1L,
        info_manga = false,
        get_chapter_updates = true,
        chapter_sort_mode = 0L,
        chapter_priority = 0L,
        download_chapters = true,
        merge_id = 50L,
        merge_url = mergeUrl,
        manga_id = 2L,
        manga_url = mangaUrl,
        manga_source = 3L,
    )

    private fun verifyInserted(times: Int) = coVerify(exactly = times) {
        harness.merged.insert(
            infoManga = true,
            getChapterUpdates = false,
            chapterSortMode = 1L,
            chapterPriority = 2L,
            downloadChapters = true,
            mergeId = 50L,
            mergeUrl = "merge://1",
            mangaId = 7L,
            mangaUrl = "/m",
            mangaSource = 3L,
        )
    }

    @Test
    fun knownReferencesAreKept() = runTest {
        every { harness.merged.selectAll() } returns fakeQuery(listOf(stored("merge://1", "/m")))
        harness.restorer().restoreMergedReferencesFor(50L, listOf(reference))
        verify(exactly = 0) { harness.graph.mangas.getMangaByUrlAndSource(any(), any()) }
    }

    @Test
    fun newReferencesNeedTheirManga() = runTest {
        every { harness.merged.selectAll() } returns
            fakeQuery(listOf(stored("merge://2", "/m"), stored("merge://1", "/other")))
        val restorer = harness.restorer()
        restorer.restoreMergedReferencesFor(50L, listOf(reference))
        verifyInserted(times = 0)
        every { harness.graph.mangas.getMangaByUrlAndSource("/m", 3L) } returns
            fakeQuery(listOf(mangasRow(id = 7L, url = "/m")))
        restorer.restoreMergedReferencesFor(50L, listOf(reference))
        verifyInserted(times = 1)
    }

    @Test
    fun flatMetadataOnlyWhenMissing() = runTest {
        val restorer = harness.restorer()
        restorer.restoreFlatMetadata(1L, backupFlatMetadata())
        coVerify(exactly = 1) { harness.graph.insertFlatMetadata.await(any<FlatMetadata>()) }
        coEvery { harness.graph.getFlatMetadataById.await(1L) } returns mockk()
        restorer.restoreFlatMetadata(1L, backupFlatMetadata())
        coVerify(exactly = 1) { harness.graph.insertFlatMetadata.await(any<FlatMetadata>()) }
    }

    @Test
    fun editedInfoOnlyWhenPresent() {
        val restorer = harness.restorer()
        restorer.restoreEditedInfo(null)
        verify(exactly = 0) { harness.graph.setCustomMangaInfo.set(any()) }
        val info = CustomMangaInfo(id = 1L, title = "T")
        restorer.restoreEditedInfo(info)
        verify { harness.graph.setCustomMangaInfo.set(info) }
    }
}
