package eu.kanade.presentation.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences

/** A real [LibrarySettingsScreenModel] over in-memory preferences and [trackerCount] logged-in trackers. */
internal class LibrarySettingsHarness(trackerCount: Int) {
    val store: FlowPreferenceStore = FlowPreferenceStore()
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val sorts: MutableList<String> = mutableListOf()
    private val trackers: List<BaseTracker> = List(trackerCount) { index ->
        mockk<BaseTracker> {
            every { id } returns index + 1L
            every { name } returns "Tracker ${index + 1}"
        }
    }
    private val setSort = mockk<SetSortModeForCategory>().also {
        coEvery { it.await(any<Category>(), any(), any()) } answers { sorts += "${secondArg<Any>()}" }
        coEvery { it.await(null as Category?, any(), any()) } answers { sorts += "${secondArg<Any>()}" }
    }
    private val trackerManager = mockk<TrackerManager>().also {
        every { it.loggedInTrackers() } returns trackers
        every { it.loggedInTrackersFlow() } returns flowOf(trackers)
    }

    val model: LibrarySettingsScreenModel = LibrarySettingsScreenModel(
        preferences = BasePreferences(ApplicationProvider.getApplicationContext<Context>(), store),
        libraryPreferences = libraryPreferences,
        setDisplayMode = SetDisplayMode(libraryPreferences),
        setSortModeForCategory = setSort,
        trackerManager = trackerManager,
    )
}
