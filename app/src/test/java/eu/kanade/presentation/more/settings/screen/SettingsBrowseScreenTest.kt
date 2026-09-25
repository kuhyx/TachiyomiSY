package eu.kanade.presentation.more.settings.screen

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.fragment.app.FragmentActivity
import eu.kanade.presentation.more.settings.screen.browse.ExtensionStoresScreen
import eu.kanade.tachiyomi.ui.category.sources.SourceCategoryScreen
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SettingsBrowseScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val storeCount = mockk<GetExtensionStoreCountAsFlow>().also { every { it() } returns flowOf(3L) }

    @Before
    fun setUp() {
        koin.start(module { single { storeCount } })
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    @Test
    fun navigationRows() {
        harness.show(SettingsBrowseScreen)
        harness.item("Edit categories").subtitle shouldBe "0 categories"
        harness.click("Edit categories")
        verify { harness.navigator.push(any<SourceCategoryScreen>()) }
        harness.item("Extension stores").subtitle shouldBe "3 stores"
        harness.click("Extension stores")
        verify { harness.navigator.push(any<ExtensionStoresScreen>()) }
    }

    @Test
    fun feedPositionFollowsHide() {
        harness.show(SettingsBrowseScreen)
        harness.item("Feed tab position").enabled shouldBe true
        koin.ui.hideFeedTab.set(true)
        compose.waitForIdle()
        harness.item("Feed tab position").enabled shouldBe false
    }

    @Test
    fun nsfwToggleAuthenticates() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        harness.show(SettingsBrowseScreen, context = activity)
        harness.switch("Show in sources and extensions lists", value = false) shouldBe true
    }
}
