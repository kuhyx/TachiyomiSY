package eu.kanade.tachiyomi.ui.browse.source.globalsearch

import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

/** Koin, sources and interactors for the global and migration search models. */
internal class SearchHarness {
    val koin: BrowseKoin = BrowseKoin()
    val sources: MutableList<CatalogueSource> = mutableListOf()
    val sourceManager: SourceManager = mockk {
        every { getVisibleSources() } answers { sources.toList() }
        every { get(any()) } answers { sources.firstOrNull { it.id == firstArg<Long>() } }
    }
    val installed: MutableStateFlow<List<Extension.Installed>> = MutableStateFlow(emptyList())
    val extensionManager: ExtensionManager = mockk { every { installedExtensionsFlow } returns installed }
    val networkToLocalManga: NetworkToLocalManga = mockk {
        coEvery { invoke(any<List<Manga>>()) } answers { firstArg() }
    }
    val getManga: GetManga = mockk()

    /** A source with [id] whose search returns [titles], or throws when [titles] is null. */
    fun source(
        sourceId: Long,
        sourceName: String = "S$sourceId",
        sourceLang: String = "en",
        titles: List<String>? = listOf("T$sourceId"),
    ): CatalogueSource =
        mockk<CatalogueSource> {
            every { id } returns sourceId
            every { name } returns sourceName
            every { lang } returns sourceLang
            every { getFilterList() } returns FilterList()
            if (titles == null) {
                coEvery { getSearchManga(1, any(), any()) } throws IllegalStateException("down")
            } else {
                coEvery { getSearchManga(1, any(), any()) } returns MangasPage(
                    titles.map { title ->
                        SManga.create().apply {
                            url = "/$title"
                            this.title = title
                        }
                    },
                    false,
                )
            }
        }.also { sources += it }

    fun start() {
        koin.sourcePreferences.enabledLanguages.set(setOf("en"))
        koin.start(
            module {
                single { sourceManager }
                single { extensionManager }
                single { networkToLocalManga }
                single { getManga }
            },
        )
    }

    fun stop() = koin.stop()
}
