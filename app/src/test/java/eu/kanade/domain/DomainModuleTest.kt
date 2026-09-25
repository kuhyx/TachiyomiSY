package eu.kanade.domain

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.koin.core.context.stopKoin
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.track.repository.TrackRepository

internal class DomainModuleTest {

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun everyDefinitionResolves() {
        val resolved = resolveEveryDefinition(listOf(DomainModule(), SYDomainModule()), domainLeaves()).first()
        resolved.size shouldBe 86
        resolved shouldContain CategoryRepository::class
        resolved shouldContain MangaRepository::class
        resolved shouldContain TrackRepository::class
    }
}
