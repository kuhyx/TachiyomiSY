package eu.kanade.tachiyomi.ui.manga.notes

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextInput
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.manga
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.interactor.UpdateMangaNotes

@RunWith(RobolectricTestRunner::class)
internal class MangaNotesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = BrowseKoin()
    private val update = mockk<UpdateMangaNotes>()

    @Before
    fun setUp() {
        coEvery { update(any(), any()) } returns true
        koin.start(module { single { update } })
    }

    @After
    fun tearDown() = koin.stop()

    @Test
    fun typedNotesAreSaved() {
        ScreenHost(MangaNotesScreen(manga().copy(notes = "old"))).show(compose)
        compose.onNode(hasSetTextAction()).performTextInput("new ")
        compose.mainClock.advanceTimeBy(1_000L)
        compose.waitForIdle()
        coVerify(timeout = 5_000) { update(1L, any()) }
    }

    @Test
    fun unchangedNotesAreNotSaved() {
        ScreenHost(MangaNotesScreen(manga().copy(notes = ""))).show(compose)
        compose.mainClock.advanceTimeBy(1_000L)
        compose.waitForIdle()
        coVerify(exactly = 0) { update(any(), any()) }
    }
}
