package mihon.domain.chapter.interactor

import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga

/** Interactor responsible for determining which chapters of a manga should be downloaded. */
public class FilterChaptersForDownload(
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId,
    private val downloadPreferences: DownloadPreferences,
    private val getCategories: GetCategories,
) {

    /**
     * Determines which chapters of a manga should be downloaded based on user preferences.
     *
     * @param manga The manga for which chapters may be downloaded.
     * @param newChapters The list of new chapters available for the manga.
     * @return A list of chapters that should be downloaded
     */
    public suspend fun await(manga: Manga, newChapters: List<Chapter>): List<Chapter> {
        val wanted = newChapters.isNotEmpty() &&
            downloadPreferences.downloadNewChapters.get() &&
            manga.shouldDownloadNewChapters()
        return when {
            !wanted -> emptyList()
            !downloadPreferences.downloadNewUnreadChaptersOnly.get() -> newChapters
            else -> newChapters.filterNot { it.chapterNumber in readChapterNumbers(manga) }
        }
    }

    // Numbers of the chapters already read, so a re-release of one is not downloaded again.
    private suspend fun readChapterNumbers(manga: Manga): Set<Double> {
        // SY -->
        val existingChapters = if (manga.source == MERGED_SOURCE_ID) {
            getMergedChaptersByMangaId.await(manga.id)
        } else {
            getChaptersByMangaId.await(manga.id)
        }
        // SY <--
        return existingChapters
            .asSequence()
            .filter { it.read && it.isRecognizedNumber }
            .map { it.chapterNumber }
            .toSet()
    }

    // Favourites only, and only in a category the download preferences include and do not exclude.
    private suspend fun Manga.shouldDownloadNewChapters(): Boolean {
        if (!favorite) return false

        val categories = getCategories.await(id).map { it.id }.ifEmpty { listOf(DEFAULT_CATEGORY_ID) }
        val includedCategories = downloadPreferences.downloadNewChapterCategories.get().map { it.toLong() }
        val excludedCategories = downloadPreferences.downloadNewChapterCategoriesExclude.get().map { it.toLong() }

        return when {
            // Default Download from all categories
            includedCategories.isEmpty() && excludedCategories.isEmpty() -> true
            // In excluded category
            categories.any { it in excludedCategories } -> false
            // Included category not selected
            includedCategories.isEmpty() -> true
            // In included category
            else -> categories.any { it in includedCategories }
        }
    }

    /** Constants of the download filter. */
    public companion object {
        private const val DEFAULT_CATEGORY_ID = 0L
    }
}
