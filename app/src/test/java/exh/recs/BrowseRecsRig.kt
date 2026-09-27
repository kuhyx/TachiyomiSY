package exh.recs

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.CannedServer
import eu.kanade.tachiyomi.source.online.networkHelperOf
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceHarness
import exh.recs.sources.ComickPagingSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga

/** Comick answering one recommendation, "Rec One" (hid h1). */
internal const val COMICK_RECS = """{"comic":{"recommendations":[{"relates":""" +
    """{"title":"Rec One","hid":"h1","md_covers":[{"b2key":"k.jpg"}]}}]}}"""

/**
 * [BrowseSourceHarness] whose source 1 is Comick, with library manga 4 on it; recommendation requests are
 * answered by [server] and results become library rows numbered from 100.
 */
internal class BrowseRecsRig {
    val browse = BrowseSourceHarness()
    val server = CannedServer()
    val loaded = MutableStateFlow(true)
    val manga: Manga = Manga.create().copy(id = 4L, source = 1L, url = "/comic/abc#", ogTitle = "Needle")

    fun start() {
        server.body = COMICK_RECS
        every { browse.source.name } returns "Comick"
        every { browse.sourceManager.isInitialized } returns loaded
        coEvery { browse.getManga.await(4L) } returns manga
        every { browse.getManga.subscribe(any<String>(), any()) } returns flowOf(null)
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<List<Manga>>()) } answers {
            firstArg<List<Manga>>().mapIndexed { index, manga -> manga.copy(id = 100L + index) }
        }
        browse.start()
        loadKoinModules(
            module {
                single { networkToLocal }
                single<NetworkHelper> { networkHelperOf(server.client) }
                single { Json { ignoreUnknownKeys = true } }
            },
        )
    }

    fun stop() = browse.stop()

    /** Recommendations for manga 4 from Comick. */
    fun singleArgs(): BrowseRecommendsScreen.Args = BrowseRecommendsScreen.Args.SingleSourceManga(
        mangaId = 4L,
        sourceId = 1L,
        recommendationSourceName = checkNotNull(ComickPagingSource::class.qualifiedName),
    )
}
