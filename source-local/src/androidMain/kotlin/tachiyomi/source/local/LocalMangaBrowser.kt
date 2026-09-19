package tachiyomi.source.local

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.LocalSourceFileSystem
import kotlin.time.Duration.Companion.days

/** Lists the manga directories of the local source, filtered by query and recency, then sorted. */
internal class LocalMangaBrowser(
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
    private val allowHiddenFiles: () -> Boolean,
) {

    /**
     * Every manga directory matching [query], or only those modified in the last week when
     * [latestOnly] is set, sorted by the [OrderBy] filter in [filters].
     */
    suspend fun search(query: String, filters: FilterList, latestOnly: Boolean): MangasPage = withIOContext {
        val lastModifiedLimit = if (latestOnly) System.currentTimeMillis() - LATEST_THRESHOLD else 0L
        val allowLocalSourceHiddenFolders = allowHiddenFiles()

        var mangaDirs = fileSystem.getFilesInBaseDirectory()
            // Filter out files that are hidden and is not a folder
            .filter { it.isDirectory && (!it.name.orEmpty().startsWith('.') || allowLocalSourceHiddenFolders) }
            .distinctBy { it.name }
            .filter { it.matches(query, lastModifiedLimit) }

        filters.forEach { filter -> mangaDirs = mangaDirs.sortedByFilter(filter) }

        val mangas = mangaDirs
            .map { mangaDir -> async { mangaOf(mangaDir) } }
            .awaitAll()

        MangasPage(mangas, false)
    }

    private fun UniFile.matches(query: String, lastModifiedLimit: Long): Boolean = when {
        lastModifiedLimit == 0L && query.isBlank() -> true
        lastModifiedLimit == 0L -> name.orEmpty().contains(query, ignoreCase = true)
        else -> lastModified() >= lastModifiedLimit
    }

    private fun List<UniFile>.sortedByFilter(filter: Any?): List<UniFile> = when (filter) {
        is OrderBy.Popular -> if (checkNotNull(filter.state).ascending) {
            sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
        } else {
            sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
        }
        is OrderBy.Latest -> if (checkNotNull(filter.state).ascending) {
            sortedBy(UniFile::lastModified)
        } else {
            sortedByDescending(UniFile::lastModified)
        }
        else -> this
    }

    private fun mangaOf(mangaDir: UniFile): SManga = SManga.create().apply {
        title = mangaDir.name.orEmpty()
        url = mangaDir.name.orEmpty()

        // Try to find the cover
        coverManager.find(mangaDir.name.orEmpty())?.let {
            thumbnail_url = it.uri.toString()
        }
    }

    private companion object {
        val LATEST_THRESHOLD: Long = 7.days.inWholeMilliseconds
    }
}
