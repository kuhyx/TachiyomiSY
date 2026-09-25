package eu.kanade.tachiyomi.source

import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.source.model.StubSource
import tachiyomi.source.local.LocalSource

internal class SourceExtensionsTest {
    private val store = InMemoryPreferenceStore()
    private val preferences = SourcePreferences(store)

    @BeforeEach
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun source(lang: String, name: String = "Src"): Source {
        val source = mockk<Source>()
        every { source.lang } returns lang
        every { source.name } returns name
        every { source.toString() } returns "$name (${lang.uppercase()})"
        return source
    }

    @Test
    fun oneLanguageHidesTag() {
        source("en").getNameForMangaInfo(enabledLanguages = listOf("en")) shouldBe "Src"
    }

    @Test
    fun oneLanguageDisabledKeepsTag() {
        source("fr").getNameForMangaInfo(enabledLanguages = listOf("en")) shouldBe "Src (FR)"
    }

    @Test
    fun manyLanguagesKeepTag() {
        source("en").getNameForMangaInfo(enabledLanguages = listOf("en", "fr")) shouldBe "Src (EN)"
    }

    @Test
    fun defaultLanguagesFromPrefs() {
        preferences.enabledLanguages.set(setOf("all", "other", "en"))
        source("en").getNameForMangaInfo() shouldBe "Src"
        preferences.enabledLanguages.set(setOf("en", "de"))
        source("en").getNameForMangaInfo() shouldBe "Src (EN)"
    }

    @Test
    fun mergedSourcesOnlyNames() {
        val merged = listOf(source("en", "A"), source("fr", "B"))
        source("en").getNameForMangaInfo(mergeSources = merged, enabledLanguages = listOf("en")) shouldBe "A, B (FR)"
    }

    @Test
    fun mergedSourcesFullNames() {
        val merged = listOf(source("en", "A"), source("fr", "B"))
        source("en").getNameForMangaInfo(merged, listOf("en", "fr")) shouldBe "A (EN), B (FR)"
    }

    @Test
    fun emptyMergedSourcesFallThrough() {
        source("en").getNameForMangaInfo(mergeSources = emptyList(), enabledLanguages = listOf("en")) shouldBe "Src"
    }

    @Test
    fun isLocalOrStub() {
        StubSource(id = 1L, lang = "en", name = "stub").isLocalOrStub() shouldBe true
        val local = mockk<Source> { every { id } returns LocalSource.ID }
        local.isLocalOrStub() shouldBe true
        val online = mockk<Source> { every { id } returns 5L }
        online.isLocalOrStub() shouldBe false
    }
}
