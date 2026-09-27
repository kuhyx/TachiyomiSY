package exh.md.follows

import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceHarness
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import mihon.domain.migration.usecases.MigrateMangaUseCase
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount

/**
 * [BrowseSourceHarness] with a MangaDex source 1 whose follows are "Fav" (in the library), "Dup" (has a
 * library duplicate) and "Plain"; library ids are 11, 12 and 13 in that order.
 */
internal class FollowsRig {
    val browse = BrowseSourceHarness()
    val mangaDex: MangaDex = mockk(relaxed = true)
    val loaded = MutableStateFlow(true)
    val migrate: MigrateMangaUseCase = mockk(relaxed = true)
    val duplicate: Manga = Manga.create().copy(id = 40L, source = 1L, ogTitle = "Older copy", url = "/old")

    fun start() {
        every { mangaDex.id } returns 1L
        every { mangaDex.name } returns "MangaDex"
        every { mangaDex.lang } returns "en"
        every { browse.sourceManager.getOrStub(1L) } returns mangaDex
        every { browse.sourceManager.isInitialized } returns loaded
        coEvery { mangaDex.fetchFollows(any()) } returns MangasPage(
            listOf("Fav", "Dup", "Plain").map { SManga(url = "/m/$it", title = it) },
            false,
        )
        every { browse.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        coEvery { browse.getDuplicates(any()) } answers {
            if (firstArg<Manga>().title == "Dup") listOf(MangaWithChapterCount(duplicate, 3L)) else emptyList()
        }
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<List<Manga>>()) } answers {
            firstArg<List<Manga>>().map { manga ->
                val id = 11L + listOf("Fav", "Dup", "Plain").indexOf(manga.title)
                manga.copy(id = id, favorite = manga.title == "Fav")
            }
        }
        val downloads = mockk<DownloadManager>()
        every { downloads.getDownloadCount(any<Manga>()) } returns 0
        browse.start()
        loadKoinModules(
            module {
                single { networkToLocal }
                single { downloads }
                single { migrate }
            },
        )
    }

    fun stop() = browse.stop()
}
