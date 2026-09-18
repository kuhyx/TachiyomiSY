package tachiyomi.domain.source.interactor

import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.domain.source.repository.SourcePagingSource
import tachiyomi.domain.source.repository.SourceRepository

internal class SourceRepositoryInteractorsTest {

    private val repository: SourceRepository = mockk()
    private val pagingSource: SourcePagingSource = mockk()
    private val filters = FilterList()

    @Test
    fun popularQueryGivesPopular() {
        every { repository.getPopular(SOURCE_ID) } returns pagingSource

        GetRemoteManga(repository)(SOURCE_ID, GetRemoteManga.QUERY_POPULAR, filters) shouldBe pagingSource
    }

    @Test
    fun latestQueryGivesLatest() {
        every { repository.getLatest(SOURCE_ID) } returns pagingSource

        GetRemoteManga(repository)(SOURCE_ID, GetRemoteManga.QUERY_LATEST, filters) shouldBe pagingSource
    }

    @Test
    fun otherQuerySearches() {
        every { repository.search(SOURCE_ID, "naruto", any()) } returns pagingSource

        GetRemoteManga(repository)(SOURCE_ID, "naruto", filters) shouldBe pagingSource
    }

    @Test
    fun sourcesWithNonLibraryManga() = runTest {
        val source = Source(id = SOURCE_ID, lang = "en", name = "Src", supportsLatest = true, isStub = false)
        val rows = listOf(SourceWithCount(source = source, count = 2L))
        every { repository.getSourcesWithNonLibraryManga() } returns flowOf(rows)

        GetSourcesWithNonLibraryManga(repository).subscribe().first() shouldContainExactly rows
    }

    private companion object {
        const val SOURCE_ID = 7L
    }
}
