package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class SyncScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val preferences = SyncPreferences(MapPreferenceStore())

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun selectorStoresToggles() {
        val before = preferences.getSyncSettings().libraryEntries
        compose.showAbove(SyncSettingsSelector())
        compose.onNodeWithText("Library entries").performClick()
        compose.waitForIdle()
        preferences.getSyncSettings().libraryEntries shouldBe !before
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        count("Below") shouldBe 1
    }

    @Test
    fun triggersStoreToggles() {
        val before = preferences.getSyncTriggerOptions().syncOnChapterRead
        compose.showAbove(SyncTriggerOptionsScreen())
        compose.onNodeWithText("Sync on Chapter Read").performClick()
        compose.waitForIdle()
        preferences.getSyncTriggerOptions().syncOnChapterRead shouldBe !before
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        count("Below") shouldBe 1
    }
}
