package tachiyomi.data.source

import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.source.model.Source as DomainSource

/** Inserts a bare manga row for [source] at [url] and returns its id. */
internal suspend fun Database.insertManga(source: Long, url: String, favorite: Boolean): Long =
    mangasQueries.insertReturningId(
        source = source,
        url = url,
        artist = null,
        author = null,
        description = null,
        genre = null,
        title = url,
        status = 0L,
        thumbnailUrl = null,
        favorite = favorite,
        lastUpdate = null,
        nextUpdate = null,
        initialized = false,
        viewerFlags = 0L,
        chapterFlags = 0L,
        coverLastModified = 0L,
        dateAdded = 0L,
        updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
        calculateInterval = 0L,
        version = 0L,
        notes = "",
        memo = JsonObject(emptyMap()),
    ).awaitAsOne()

internal class SourceRepositoryCountsTest {
    private val database = inMemoryDatabase()
    private val installed = mockSource(sourceId = 1L)
    private val stub = StubSource(id = 2L, lang = "", name = "")
    private val sourceManager = mockk<SourceManager> {
        every { sources } returns flowOf(emptyList())
        every { getOrStub(1L) } returns installed
        every { getOrStub(2L) } returns stub
    }
    private val repository = SourceRepositoryImpl(sourceManager, database)

    private val installedDomain = DomainSource(
        id = 1L,
        lang = "en",
        name = "Source 1",
        supportsLatest = false,
        isStub = false,
    )
    private val stubDomain = DomainSource(id = 2L, lang = "", name = "", supportsLatest = false, isStub = true)

    @Test
    fun favoriteCountsSkipMerged() = runTest {
        database.insertManga(source = 1L, url = "a", favorite = true)
        database.insertManga(source = 1L, url = "b", favorite = true)
        database.insertManga(source = 2L, url = "c", favorite = true)
        database.insertManga(source = MERGED_SOURCE_ID, url = "m", favorite = true)
        database.insertManga(source = 1L, url = "d", favorite = false)

        val counts = repository.getSourcesWithFavoriteCount().first().sortedBy { it.first.id }

        counts shouldBe listOf(installedDomain to 2L, stubDomain to 1L)
    }

    @Test
    fun favoriteCountsOfEmptyLibrary() = runTest {
        repository.getSourcesWithFavoriteCount().first().shouldBeEmpty()
    }

    @Test
    fun nonLibraryCountsPerSource() = runTest {
        database.insertManga(source = 1L, url = "a", favorite = false)
        database.insertManga(source = 2L, url = "b", favorite = false)
        database.insertManga(source = 2L, url = "c", favorite = false)
        database.insertManga(source = 2L, url = "d", favorite = true)

        val counts = repository.getSourcesWithNonLibraryManga().first().sortedBy { it.id }

        counts shouldBe listOf(SourceWithCount(installedDomain, 1L), SourceWithCount(stubDomain, 2L))
        counts.map { it.name } shouldBe listOf("Source 1", "")
    }

    @Test
    fun nonLibraryCountsOfEmptyTable() = runTest {
        repository.getSourcesWithNonLibraryManga().first().shouldBeEmpty()
    }
}
