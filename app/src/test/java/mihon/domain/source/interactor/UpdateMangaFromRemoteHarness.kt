package mihon.domain.source.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import java.io.File

/** The remote-update interactor with every collaborator mocked; the mocks are reachable as properties. */
internal class UpdateMangaFromRemoteHarness {
    val sourceManager = mockk<SourceManager>()
    val chapterRepository = mockk<ChapterRepository>()
    val mangaRepository = mockk<MangaRepository>()
    val syncChaptersWithSource = mockk<SyncChaptersWithSource>()
    val coverCache = mockk<CoverCache>()
    val libraryPreferences = LibraryPreferences(FlowPreferenceStore())
    val downloadManager = mockk<DownloadManager>()
    val interactor = UpdateMangaFromRemote(
        UpdateMangaFromRemote.Collaborators(
            sourceManager = sourceManager,
            chapterRepository = chapterRepository,
            mangaRepository = mangaRepository,
            syncChaptersWithSource = syncChaptersWithSource,
            coverCache = coverCache,
            libraryPreferences = libraryPreferences,
            downloadManager = downloadManager,
        ),
    )

    /** The updates written to the manga repository, in order. */
    val mangaUpdates = mutableListOf<MangaUpdate>()

    /** A plain source answering [remote] for manga 4, whose stored chapters are [stored]. */
    fun plainSource(remote: SManga, stored: List<Chapter> = emptyList()): Source {
        val source = mockk<Source> { every { id } returns 7L }
        coEvery { source.getMangaUpdate(any(), any(), any(), any()) } returns SMangaUpdate(remote, remoteChapters)
        coEvery { chapterRepository.getChapterByMangaId(4) } returns stored
        coEvery { mangaRepository.update(capture(mangaUpdates)) } returns true
        coEvery { mangaRepository.getMangaById(4) } returns updatedManga
        coEvery {
            syncChaptersWithSource.await(remoteChapters, any(), source, any(), any())
        } returns listOf(Chapter.create().copy(id = 9))
        every { coverCache.getCustomCoverFile(4) } returns File("missing-custom-cover")
        every { coverCache.deleteFromCache(any(), false) } returns 0
        return source
    }

    companion object {
        val remoteChapters: List<SChapter> = listOf(SChapter.create().apply { url = "/c1" })
        val updatedManga: Manga = Manga.create().copy(id = 4, source = 7, ogTitle = "Updated")
    }
}

/** A remote manga carrying [title] and [thumbnail]. */
internal fun remoteManga(title: String? = "Remote", thumbnail: String? = "http://cover"): SManga =
    SManga.create().apply {
        title?.let { this.title = it }
        thumbnail_url = thumbnail
        author = "A"
        status = SManga.ONGOING
    }

/** The stored copy of manga 4 on source 7. */
internal fun localManga(favorite: Boolean = false, thumbnail: String? = "http://old", source: Long = 7): Manga =
    Manga.create().copy(id = 4, source = source, ogTitle = "Local", ogThumbnailUrl = thumbnail, favorite = favorite)
