package tachiyomi.domain.source.model

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate

/**
 * Stand-in for a source whose extension is not installed: keeps the id, name and language so
 * library entries can still be listed, and throws [SourceNotInstalledException] on every fetch.
 */
public class StubSource(
    override val id: Long,
    override val lang: String,
    override val name: String,
) : Source {

    private val isInvalid: Boolean = name.isBlank() || lang.isBlank()

    override val supportsLatest: Boolean = false

    override suspend fun getPopularManga(page: Int): MangasPage = throw SourceNotInstalledException()

    override suspend fun getLatestUpdates(page: Int): MangasPage = throw SourceNotInstalledException()

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        throw SourceNotInstalledException()

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = throw SourceNotInstalledException()

    override suspend fun getPageList(chapter: SChapter): List<Page> =
        throw SourceNotInstalledException()

    override fun toString(): String =
        if (!isInvalid) "$name (${lang.uppercase()})" else id.toString()

    /** Factory for stubs of existing sources. */
    public companion object {
        /** A stub carrying [source]'s id, language and name. */
        public fun from(source: Source): StubSource = StubSource(id = source.id, lang = source.lang, name = source.name)
    }
}

/** Thrown by [StubSource] when a fetch is attempted on a source that is not installed. */
public class SourceNotInstalledException : Exception()
