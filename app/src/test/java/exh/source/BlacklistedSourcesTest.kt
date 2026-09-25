package exh.source

import eu.kanade.tachiyomi.source.online.sourceIdOf
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class BlacklistedSourcesTest {
    @Test
    fun extensionIdsCoverLanguages() {
        BlacklistedSources.EHENTAI_EXT_SOURCES.size shouldBe 17
        val english = sourceIdOf(name = "E-Hentai", lang = "en", versionId = 1)
        BlacklistedSources.EHENTAI_EXT_SOURCES.first() shouldBe english
        BlacklistedSources.BLACKLISTED_EXT_SOURCES shouldBe BlacklistedSources.EHENTAI_EXT_SOURCES
    }

    @Test
    fun extensionAndHiddenSourceLists() {
        BlacklistedSources.BLACKLISTED_EXTENSIONS.toList() shouldContainExactly
            listOf("eu.kanade.tachiyomi.extension.all.ehentai")
        BlacklistedSources.HIDDEN_SOURCES shouldBe setOf(MERGED_SOURCE_ID)
        val saved = BlacklistedSources.HIDDEN_SOURCES
        BlacklistedSources.HIDDEN_SOURCES = emptySet()
        BlacklistedSources.HIDDEN_SOURCES shouldBe emptySet()
        BlacklistedSources.HIDDEN_SOURCES = saved
    }
}
