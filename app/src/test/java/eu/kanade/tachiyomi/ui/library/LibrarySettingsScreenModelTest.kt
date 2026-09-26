package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort

@RunWith(RobolectricTestRunner::class)
internal class LibrarySettingsScreenModelTest {
    private val harness = LibraryHarness()
    private val setDisplayMode = mockk<SetDisplayMode>(relaxed = true)
    private val setSortMode = mockk<SetSortModeForCategory>(relaxed = true)
    private val tracker = mockk<BaseTracker>()
    private val model by lazy {
        every { harness.trackerManager.loggedInTrackers() } returns listOf(tracker)
        LibrarySettingsScreenModel(
            preferences = harness.basePreferences,
            libraryPreferences = harness.libraryPreferences,
            setDisplayMode = setDisplayMode,
            setSortModeForCategory = setSortMode,
            trackerManager = harness.trackerManager,
        )
    }

    @Before
    fun setUp() {
        mainUnconfined()
    }

    @After
    fun tearDown() = mainReset()

    @Test
    fun trackersStartLoggedIn() {
        model.trackersFlow.value shouldBe listOf(tracker)
    }

    @Test
    fun injectedDefaults() {
        startKoin {
            modules(
                harness.koinModules() + module {
                    single { setDisplayMode }
                    single { setSortMode }
                },
            )
        }
        try {
            LibrarySettingsScreenModel().setDisplayMode(LibraryDisplayMode.List)
            verify { setDisplayMode.await(LibraryDisplayMode.List) }
        } finally {
            stopKoin()
        }
    }

    @Test
    fun filtersCycle() {
        model.toggleFilter { it.filterUnread }
        harness.libraryPreferences.filterUnread.get() shouldBe TriState.ENABLED_IS
        model.toggleTracker(7)
        harness.libraryPreferences.filterTracking(7).get() shouldBe TriState.ENABLED_IS
    }

    @Test
    fun displayAndSort() {
        model.setDisplayMode(LibraryDisplayMode.List)
        verify { setDisplayMode.await(LibraryDisplayMode.List) }
        model.setSort(null, LibrarySort.Type.DateAdded, LibrarySort.Direction.Descending)
        coVerify(timeout = WAIT) {
            setSortMode.await(category = null, type = LibrarySort.Type.DateAdded, direction = any())
        }
    }

    @Test
    fun groupingIsSaved() {
        model.grouping shouldBe LibraryGroup.BY_DEFAULT
        model.setGrouping(LibraryGroup.BY_SOURCE)
        harness.libraryPreferences.groupLibraryBy.changes().await { it == LibraryGroup.BY_SOURCE }
    }

    private companion object {
        const val WAIT = 5_000L
    }
}
