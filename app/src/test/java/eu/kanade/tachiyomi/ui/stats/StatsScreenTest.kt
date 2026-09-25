package eu.kanade.tachiyomi.ui.stats

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.customInfoModule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.history.interactor.GetTotalReadDuration
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetReadMangaNotInLibraryView
import tachiyomi.domain.track.interactor.GetTracks

@RunWith(RobolectricTestRunner::class)
internal class StatsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val getLibraryManga = mockk<GetLibraryManga> { coEvery { await() } returns emptyList() }

    @Before
    fun setUp() {
        startKoin {
            modules(
                customInfoModule(),
                module {
                    single { mockk<DownloadManager> { every { getDownloadCount() } returns 0 } }
                    single { getLibraryManga }
                    single { mockk<GetTotalReadDuration> { coEvery { await() } returns 0L } }
                    single { mockk<GetTracks>() }
                    single { LibraryPreferences(MapPreferenceStore()) }
                    single { mockk<TrackerManager> { every { loggedInTrackers() } returns emptyList() } }
                    single { mockk<GetReadMangaNotInLibraryView> { coEvery { await() } returns emptyList() } }
                },
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun overflowTogglesAllRead() {
        compose.setContent { ScreenHost(StatsScreen()) }
        compose.onNodeWithText("Statistics").assertExists()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Include all read entries").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Ignore non-library entries").assertExists()
    }

    @Test
    fun loadingShowsTheSpinner() {
        coEvery { getLibraryManga.await() } coAnswers { awaitCancellation() }
        compose.setContent { ScreenHost(StatsScreen()) }
        compose.onNodeWithText("Statistics").assertExists()
    }
}
