package exh.source

import eu.kanade.tachiyomi.source.AndroidSourceManager
import eu.kanade.tachiyomi.source.online.all.Lanraragi
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.source.online.all.NHentai
import eu.kanade.tachiyomi.source.online.english.EightMuses
import eu.kanade.tachiyomi.source.online.english.HBrowse
import eu.kanade.tachiyomi.source.online.english.Pururin
import eu.kanade.tachiyomi.source.online.english.Tsumino
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class SourceHelperTest {
    private val savedDelegated = metadataDelegatedSourceIds
    private val savedNHentai = nHentaiSourceIds
    private val savedMangaDex = mangaDexSourceIds
    private val savedLanraragi = lanraragiSourceIds
    private val savedExcluded = LIBRARY_UPDATE_EXCLUDED_SOURCES
    private val savedSources = AndroidSourceManager.currentDelegatedSources.toMap()

    @BeforeEach
    fun setUp() = AndroidSourceManager.currentDelegatedSources.clear()

    @AfterEach
    fun tearDown() {
        metadataDelegatedSourceIds = savedDelegated
        nHentaiSourceIds = savedNHentai
        mangaDexSourceIds = savedMangaDex
        lanraragiSourceIds = savedLanraragi
        LIBRARY_UPDATE_EXCLUDED_SOURCES = savedExcluded
        AndroidSourceManager.currentDelegatedSources.clear()
        AndroidSourceManager.currentDelegatedSources.putAll(savedSources)
    }

    private fun delegate(id: Long, source: KClass<out DelegatedHttpSource>) {
        AndroidSourceManager.currentDelegatedSources[id] = AndroidSourceManager.Companion.DelegatedSource(
            sourceName = "s$id",
            sourceId = id,
            originalSourceQualifiedClassName = "c$id",
            newSourceClass = source,
        )
    }

    /** Adding a delegated source runs [handleSourceLibrary] through the listening map. */
    @Test
    fun delegatedSourcesFillTheIdLists() {
        delegate(30L, NHentai::class)
        delegate(20L, MangaDex::class)
        delegate(10L, Lanraragi::class)
        delegate(40L, MangaDex::class)
        delegate(50L, Pururin::class)
        delegate(60L, Tsumino::class)
        delegate(70L, HBrowse::class)
        delegate(80L, EightMuses::class)
        nHentaiSourceIds shouldContainExactly listOf(30L)
        mangaDexSourceIds shouldContainExactly listOf(20L, 40L)
        lanraragiSourceIds shouldContainExactly listOf(10L)
        metadataDelegatedSourceIds shouldContainExactly listOf(10L, 30L, 50L, 60L, 70L, 80L)
        LIBRARY_UPDATE_EXCLUDED_SOURCES shouldContainExactly
            listOf(EH_SOURCE_ID, EXH_SOURCE_ID, PURURIN_SOURCE_ID, 30L)
    }

    @Test
    fun emptyLibraryClearsTheIdLists() {
        delegate(30L, NHentai::class)
        AndroidSourceManager.currentDelegatedSources.remove(30L)
        nHentaiSourceIds shouldContainExactly emptyList()
        mangaDexSourceIds shouldContainExactly emptyList()
        lanraragiSourceIds shouldContainExactly emptyList()
        metadataDelegatedSourceIds shouldContainExactly emptyList()
        LIBRARY_UPDATE_EXCLUDED_SOURCES shouldContainExactly
            listOf(EH_SOURCE_ID, EXH_SOURCE_ID, PURURIN_SOURCE_ID)
    }
}
