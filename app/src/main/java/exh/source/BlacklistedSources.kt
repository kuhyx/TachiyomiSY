package exh.source

import eu.kanade.tachiyomi.source.online.sourceIdOf

internal object BlacklistedSources {
    /** The languages the E-Hentai extension ships a source for; SY replaces every one of them. */
    private val EHENTAI_EXT_LANGUAGES = listOf(
        "en", "ja", "zh", "nl", "fr", "de", "hu", "it", "ko", "pl", "pt", "ru", "es", "th", "vi", "other", "none",
    )

    val EHENTAI_EXT_SOURCES = EHENTAI_EXT_LANGUAGES
        .map { sourceIdOf(name = "E-Hentai", lang = it, versionId = 1) }
        .toLongArray()

    val BLACKLISTED_EXT_SOURCES = EHENTAI_EXT_SOURCES

    val BLACKLISTED_EXTENSIONS = arrayOf(
        "eu.kanade.tachiyomi.extension.all.ehentai",
    )

    var HIDDEN_SOURCES = setOf(
        MERGED_SOURCE_ID,
    )
}
