package eu.kanade.tachiyomi.extension.en.pururin

import eu.kanade.tachiyomi.source.online.FakeDelegateSource

/** Named exactly like the Pururin extension's source, so the source manager delegates it to the in-app Pururin. */
internal class Pururin(baseUrl: String) : FakeDelegateSource(baseUrl, lang = "en", name = "Pururin fake")
