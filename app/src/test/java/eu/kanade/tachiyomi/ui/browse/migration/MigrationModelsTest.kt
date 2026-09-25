package eu.kanade.tachiyomi.ui.browse.migration

import eu.kanade.domain.source.interactor.GetSourcesWithFavoriteCount
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreenModel
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrationMangaEvent
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrateSourceScreenModel
import eu.kanade.tachiyomi.ui.browse.source.source
import eu.kanade.tachiyomi.ui.manga.eventually
import eu.kanade.tachiyomi.ui.manga.manga
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.source.service.SourceManager

@RunWith(RobolectricTestRunner::class)
internal class MigrationModelsTest {
    private val koin = BrowseKoin()
    private val counts = mockk<GetSourcesWithFavoriteCount>()
    private val sorting = mockk<SetMigrateSorting>(relaxed = true)
    private val favorites = mockk<GetFavorites>()
    private val plain = mockk<Source> { every { id } returns 7L }

    @Before
    fun setUp() = koin.start(
        module {
            single { counts }
            single { sorting }
            single { favorites }
            single<SourceManager> { mockk { every { getOrStub(7L) } returns plain } }
        },
    )

    @After
    fun tearDown() = koin.stop()

    @Test
    fun sourcesAreCounted() {
        every { counts.subscribe() } returns MutableStateFlow(listOf(source(1L) to 3L))
        val model = MigrateSourceScreenModel()
        eventually { !model.state.value.isLoading }
        model.state.value.items shouldBe listOf(source(1L) to 3L)
        model.state.value.isEmpty shouldBe false
    }

    @Test
    fun sourceFailuresAreReported() {
        every { counts.subscribe() } returns flow { error("db") }
        val model = MigrateSourceScreenModel()
        val event = runBlocking { withTimeout(10_000L) { model.channel.first() } }
        event shouldBe MigrateSourceScreenModel.Event.FailedFetchingSourcesWithCount
    }

    @Test
    fun sortingToggles() {
        every { counts.subscribe() } returns MutableStateFlow(emptyList())
        val model = MigrateSourceScreenModel()
        model.toggleSortingMode()
        verify { sorting.await(SetMigrateSorting.Mode.TOTAL, SetMigrateSorting.Direction.ASCENDING) }
        model.toggleSortingDirection()
        verify { sorting.await(SetMigrateSorting.Mode.ALPHABETICAL, SetMigrateSorting.Direction.DESCENDING) }
        koin.sourcePreferences.migrationSortingMode.set(SetMigrateSorting.Mode.TOTAL)
        koin.sourcePreferences.migrationSortingDirection.set(SetMigrateSorting.Direction.DESCENDING)
        eventually { model.state.value.sortingMode == SetMigrateSorting.Mode.TOTAL }
        eventually { model.state.value.sortingDirection == SetMigrateSorting.Direction.DESCENDING }
        model.toggleSortingMode()
        model.toggleSortingDirection()
        verify { sorting.await(SetMigrateSorting.Mode.ALPHABETICAL, SetMigrateSorting.Direction.DESCENDING) }
        verify { sorting.await(SetMigrateSorting.Mode.TOTAL, SetMigrateSorting.Direction.ASCENDING) }
    }

    @Test
    fun favouritesAreSortedAndSelectable() {
        val b = manga().copy(id = 2L, ogTitle = "b")
        val a = manga().copy(id = 3L, ogTitle = "A")
        every { favorites.subscribe(7L) } returns MutableStateFlow(listOf(b, a))
        val model = MigrateMangaScreenModel(7L)
        eventually { !model.state.value.isLoading }
        model.state.value.titles shouldBe listOf(a, b)
        model.state.value.source shouldBe plain
        model.toggleSelection(a)
        model.state.value.selectionMode shouldBe true
        model.toggleSelection(a)
        model.state.value.selection shouldBe emptySet()
        model.toggleSelection(b)
        model.clearSelection()
        model.state.value.isEmpty shouldBe false
    }

    @Test
    fun favouriteFailuresEmptyTheList() {
        every { favorites.subscribe(7L) } returns flow { error("db") }
        val model = MigrateMangaScreenModel(7L)
        val event = runBlocking { withTimeout(10_000L) { model.events.first() } }
        event shouldBe MigrationMangaEvent.FailedFetchingFavorites
        eventually { model.state.value.titles.isEmpty() && !model.state.value.isLoading }
        MigrateMangaScreenModel.State().isLoading shouldBe true
        MigrateMangaScreenModel.State(source = plain).isEmpty shouldBe true
    }
}
