package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.setupTask
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags

@RunWith(RobolectricTestRunner::class)
internal class SettingsLibraryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val resetFlags = mockk<ResetCategoryFlags> { coEvery { await() } just Runs }
    private val getCategories = mockk<GetCategories> {
        every { subscribe() } returns flowOf(listOf(defaultCategory, comics, novels))
    }

    @Before
    fun setUp() {
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every { LibraryUpdateJob.setupTask(any(), any()) } just Runs
        koin.start(
            module {
                single { resetFlags }
                single { getCategories }
            },
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    @Test
    fun categoriesGroupCallbacks() {
        harness.show(SettingsLibraryScreen)
        harness.item("Edit categories").subtitle shouldBe "2 categories"
        harness.click("Edit categories")
        verify { harness.navigator.push(any<CategoryScreen>()) }
        harness.switch("Per-category settings for sort", value = true) shouldBe true
        coVerify(exactly = 0) { resetFlags.await() }
        harness.switch("Per-category settings for sort", value = false) shouldBe true
        coVerify { resetFlags.await() }
    }

    @Test
    fun updateIntervalSchedules() {
        harness.show(SettingsLibraryScreen)
        harness.item("Automatic updates device restrictions").enabled shouldBe false
        harness.list("Automatic updates", 12) shouldBe true
        verify { LibraryUpdateJob.setupTask(any(), 12) }
        koin.library.autoUpdateInterval.set(12)
        compose.waitForIdle()
        harness.item("Automatic updates device restrictions").enabled shouldBe true
    }

    @Test
    fun restrictionsReschedule() {
        harness.show(SettingsLibraryScreen)
        val item = harness.item("Automatic updates device restrictions")
        harness.multi(item.title, setOf("wifi")) shouldBe true
        ShadowLooper.idleMainLooper()
        verify { LibraryUpdateJob.setupTask(any(), null) }
    }

    @Test
    fun categoriesDialogStoresChoice() {
        harness.show(SettingsLibraryScreen)
        harness.click("Categories")
        compose.onNodeWithText("Comics").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        koin.library.updateCategories.get() shouldBe setOf("1")
        koin.library.updateCategoriesExclude.get() shouldBe emptySet()
        harness.click("Categories")
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        harness.count("Cancel") shouldBe 0
    }

    @Test
    fun sortTagsOpensScreen() {
        harness.show(SettingsLibraryScreen)
        harness.item("Tag sorting tags").subtitle.toString() shouldStartWith "0 tags"
        harness.click("Tag sorting tags")
        verify { harness.navigator.push(any<SortTagScreen>()) }
    }
}
