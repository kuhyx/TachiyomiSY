package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.source.service.SourceManager

private fun sourceNamed(id: Long, name: String): Source = mockk<Source>().also {
    every { it.id } returns id
    every { it.name } returns name
}

internal class SourcesBackupCreatorTest {

    private val sourceManager = mockk<SourceManager>()

    @BeforeEach
    fun setUp() {
        every { sourceManager.getOrStub(any()) } answers { sourceNamed(id = firstArg(), name = "S${firstArg<Long>()}") }
        startKoin { modules(module { single { sourceManager } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun deduplicatesSourceIds() {
        val mangas = listOf(
            BackupManga(source = 1L, url = "/a"),
            BackupManga(source = 1L, url = "/b"),
            BackupManga(source = 2L, url = "/c"),
        )
        val creator = SourcesBackupCreator(sourceManager = sourceManager)
        creator(mangas).map { it.sourceId } shouldContainExactly listOf(1L, 2L)
    }

    @Test
    fun injectsSourceManagerByDefault() {
        SourcesBackupCreator()(listOf(BackupManga(source = 9L, url = "/a"))).single().name shouldBe "S9"
    }

    @Test
    fun emptyWhenNoManga() {
        SourcesBackupCreator(sourceManager = sourceManager)(emptyList()) shouldBe emptyList()
    }

    @Test
    fun toBackupSourceCopiesIdAndName() {
        val backup = sourceNamed(id = 4L, name = "Name").toBackupSource()
        backup.sourceId shouldBe 4L
        backup.name shouldBe "Name"
    }
}
