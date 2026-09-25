package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import eu.kanade.tachiyomi.data.backup.models.BackupMergedMangaReference
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import io.mockk.every
import io.mockk.mockk
import tachiyomi.data.Chapters
import tachiyomi.data.ChaptersQueries
import tachiyomi.data.Database
import tachiyomi.data.Excluded_scanlatorsQueries
import tachiyomi.data.Manga_syncQueries
import tachiyomi.data.MergedQueries
import tachiyomi.domain.category.model.Category

/**
 * A mocked [Database] with the four query sets [MangaBackupCreator] touches, each stubbed to yield
 * rows from a driverless query.
 */
internal class FakeBackupDatabase {
    val chapters: ChaptersQueries = mockk()
    val excludedScanlators: Excluded_scanlatorsQueries = mockk()
    val mangaSync: Manga_syncQueries = mockk()
    val merged: MergedQueries = mockk()
    val database: Database = mockk()

    init {
        every { database.chaptersQueries } returns chapters
        every { database.excluded_scanlatorsQueries } returns excludedScanlators
        every { database.manga_syncQueries } returns mangaSync
        every { database.mergedQueries } returns merged
        withExcludedScanlators(emptyList())
        withBackupChapters(emptyList())
        withTracks(emptyList())
        withMergedReferences(emptyList())
    }

    fun withExcludedScanlators(rows: List<String>) {
        every { excludedScanlators.getExcludedScanlatorsByMangaId(any()) } returns fakeQuery(rows)
    }

    fun withBackupChapters(rows: List<BackupChapter>) {
        every { chapters.getChaptersByMangaId<BackupChapter>(any(), any(), any()) } returns fakeQuery(rows)
    }

    fun withChapterRow(row: Chapters) {
        every { chapters.getChapterById(any()) } returns fakeQuery(listOf(row))
    }

    fun withTracks(rows: List<BackupTracking>) {
        every { mangaSync.getTracksByMangaId<BackupTracking>(any(), any()) } returns fakeQuery(rows)
    }

    fun withMergedReferences(rows: List<BackupMergedMangaReference>) {
        every { merged.selectByMergeId<BackupMergedMangaReference>(any(), any()) } returns fakeQuery(rows)
    }
}

/** A library category with a distinguishable name and order. */
internal fun testCategory(id: Long, name: String = "C$id", order: Long = id): Category = Category(
    id = id,
    name = name,
    order = order,
    flags = 0L,
)
