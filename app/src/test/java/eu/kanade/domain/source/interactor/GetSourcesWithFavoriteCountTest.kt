package eu.kanade.domain.source.interactor

import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.source.service.SourcePreferences
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.LocalSource

internal class GetSourcesWithFavoriteCountTest {

    private val preferences = SourcePreferences(FlowPreferenceStore())
    private val repository = mockk<SourceRepository>()
    private val interactor = GetSourcesWithFavoriteCount(repository, preferences)

    private val counts = listOf(
        source(1, name = "beta") to 5L,
        source(2, name = "Alpha") to 1L,
        source(3, name = "stub", isStub = true) to 9L,
        source(LocalSource.ID, name = "Local") to 2L,
    )

    @Test
    fun alphabeticalAscStubsFirst() = runTest {
        every { repository.getSourcesWithFavoriteCount() } returns flowOf(counts)
        interactor.subscribe().first().map { it.first.name } shouldBe listOf("stub", "Alpha", "beta")
    }

    @Test
    fun alphabeticalDescStubsLast() = runTest {
        every { repository.getSourcesWithFavoriteCount() } returns flowOf(counts)
        preferences.migrationSortingDirection.set(SetMigrateSorting.Direction.DESCENDING)
        interactor.subscribe().first().map { it.first.name } shouldBe listOf("beta", "Alpha", "stub")
    }

    @Test
    fun totalSortsByCount() = runTest {
        every { repository.getSourcesWithFavoriteCount() } returns flowOf(counts)
        preferences.migrationSortingMode.set(SetMigrateSorting.Mode.TOTAL)
        interactor.subscribe().first().map { it.second } shouldBe listOf(9L, 1L, 5L)
        preferences.migrationSortingDirection.set(SetMigrateSorting.Direction.DESCENDING)
        interactor.subscribe().first().map { it.second } shouldBe listOf(5L, 1L, 9L)
    }
}
