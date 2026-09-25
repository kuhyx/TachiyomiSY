package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.interactor.GetCategories

@RunWith(RobolectricTestRunner::class)
internal class SettingsDownloadScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val getCategories = mockk<GetCategories> {
        every { subscribe() } returns flowOf(listOf(defaultCategory, comics))
    }

    @Before
    fun setUp() {
        koin.start(module { single { getCategories } })
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    @Test
    fun slidersStoreLimits() {
        harness.show(SettingsDownloadScreen)
        harness.slide("Concurrent source downloads", value = 3)
        harness.slide("Concurrent page downloads", value = 7)
        koin.download.parallelSourceLimit.get() shouldBe 3
        koin.download.parallelPageLimit.get() shouldBe 7
    }

    @Test
    fun autoDownloadFollowsSwitch() {
        harness.show(SettingsDownloadScreen)
        harness.item("Download new chapters").enabled shouldBe true
        harness.item("Categories").enabled shouldBe false
        koin.download.downloadNewChapters.set(true)
        compose.waitForIdle()
        harness.item("Categories").enabled shouldBe true
    }

    @Test
    fun categoriesDialogStoresChoice() {
        koin.download.downloadNewChapters.set(true)
        harness.show(SettingsDownloadScreen)
        harness.click("Categories")
        compose.onNodeWithText("Comics").performClick()
        compose.onNodeWithText("Comics").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        koin.download.downloadNewChapterCategories.get() shouldBe emptySet()
        koin.download.downloadNewChapterCategoriesExclude.get() shouldBe setOf("1")
        harness.click("Categories")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("Cancel") shouldBe 0
    }

    @Test
    fun downloadAheadEntries() {
        harness.show(SettingsDownloadScreen)
        harness.list("Auto download while reading", 2) shouldBe true
        koin.download.autoDownloadWhileReading.set(2)
        compose.waitForIdle()
        harness.count("Next 2 unread chapters") shouldBe 1
    }
}
