package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.network.NetworkHelper
import exh.md.handlers.ApiMangaParser
import exh.md.handlers.AzukiHandler
import exh.md.handlers.BilibiliHandler
import exh.md.handlers.ComikeyHandler
import exh.md.handlers.FollowsHandler
import exh.md.handlers.MangaHandler
import exh.md.handlers.MangaHotHandler
import exh.md.handlers.MangaPlusHandler
import exh.md.handlers.NamicomiHandler
import exh.md.handlers.PageHandler
import exh.md.handlers.SimilarHandler
import exh.md.service.MangaDexAuthService
import exh.md.service.MangaDexService
import exh.md.service.SimilarService
import uy.kohesive.injekt.api.get

// The MangaDex API services and per-concern handlers, built lazily on first use.
internal class MangaDexHandlers(private val source: MangaDex, private val network: NetworkHelper) {
    val mangadexService by lazy {
        MangaDexService(source.client, source.headers)
    }

    val mangadexAuthService by lazy {
        MangaDexAuthService(source.baseHttpClient, source.headers)
    }

    val similarService by lazy {
        SimilarService(source.client)
    }

    val apiMangaParser by lazy {
        ApiMangaParser(source.mdLang.lang)
    }

    val followsHandler by lazy {
        FollowsHandler(source.mdLang.lang, mangadexAuthService)
    }

    val mangaHandler by lazy {
        MangaHandler(source.mdLang.lang, mangadexService, apiMangaParser)
    }

    val similarHandler by lazy {
        SimilarHandler(source.mdLang.lang, mangadexService, similarService)
    }

    val mangaPlusHandler by lazy {
        MangaPlusHandler(network.client)
    }

    val comikeyHandler by lazy {
        ComikeyHandler(network.client, network.defaultUserAgentProvider())
    }

    val bilibiliHandler by lazy {
        BilibiliHandler(network.client)
    }

    val azukHandler by lazy {
        AzukiHandler(network.client, network.defaultUserAgentProvider())
    }

    val mangaHotHandler by lazy {
        MangaHotHandler(network.client, network.defaultUserAgentProvider())
    }

    val namicomiHandler by lazy {
        NamicomiHandler(network.client, network.defaultUserAgentProvider())
    }

    val pageHandler by lazy {
        PageHandler(
            mangadexService,
            PageHandler.ExternalHandlers(
                mangaPlus = mangaPlusHandler,
                comikey = comikeyHandler,
                bilibili = bilibiliHandler,
                azuki = azukHandler,
                mangaHot = mangaHotHandler,
                namicomi = namicomiHandler,
            ),
        )
    }
}
