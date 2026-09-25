package eu.kanade.domain

import eu.kanade.tachiyomi.source.online.MetadataSource
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.manga.repository.MangaMergeRepository
import tachiyomi.domain.source.repository.SavedSearchRepository

internal class SYDomainModuleTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun everyDefinitionResolves() {
        val resolved = resolveEveryDefinition(listOf(SYDomainModule(), DomainModule()), domainLeaves()).first()
        resolved.size shouldBe 65
        resolved shouldContain MetadataSource.GetMangaId::class
        resolved shouldContain MangaMergeRepository::class
        resolved shouldContain SavedSearchRepository::class
        resolved shouldContain CustomMangaRepository::class
    }
}
