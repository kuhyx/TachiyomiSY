package eu.kanade.tachiyomi.extension.all.nhentai

import eu.kanade.tachiyomi.source.online.FakeDelegateSource

/** Named like the nhentai extension's factory sources, so the source manager delegates it to the in-app NHentai. */
internal class NHentaiFake(baseUrl: String) : FakeDelegateSource(baseUrl, lang = "all", name = "NHentai fake")
