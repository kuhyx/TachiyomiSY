package exh.recs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.library.hasLabel
import eu.kanade.tachiyomi.ui.library.waitForLabel
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class BrowseRecommendsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = BrowseRecsRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun show(external: Boolean) {
        compose.setContent { ScreenHost(BrowseRecommendsScreen(rig.singleArgs(), isExternalSource = external)) }
        compose.waitForLabel("Comick (Community recommendations)")
        compose.waitForLabel("Rec One")
    }

    private fun hold() {
        compose.onNodeWithText("Rec One").performTouchInput { longClick() }
        compose.waitForIdle()
    }

    @Test
    fun aSpinnerShowsUntilSourcesLoad() {
        rig.loaded.value = false
        compose.setContent { ScreenHost(BrowseRecommendsScreen(rig.singleArgs(), isExternalSource = false)) }
        compose.waitForIdle()
        compose.hasLabel("Comick (Community recommendations)") shouldBe false
        rig.loaded.value = true
        compose.waitForLabel("Rec One")
    }

    @Test
    fun localEntriesOpenTheManga() {
        show(external = false)
        compose.onNodeWithText("Rec One").performClick()
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun localHoldAlsoOpensTheManga() {
        show(external = false)
        hold()
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun externalEntriesSearchSources() {
        show(external = true)
        compose.onNodeWithText("Rec One").performClick()
        compose.waitForLabel("opened:SourcesScreen")
    }

    @Test
    fun externalHoldOpensTheWebView() {
        show(external = true)
        hold()
        val started = shadowOf(compose.activity).nextStartedActivity
        started.component?.className shouldBe WebViewActivity::class.java.name
        started.getStringExtra("url_key") shouldBe "/comic/h1#"
    }

    @Test
    fun displayModeAndBack() {
        show(external = false)
        compose.onNodeWithContentDescription("Display mode").performClick()
        compose.waitForLabel("List")
        compose.onNodeWithText("List").performClick()
        compose.waitForIdle()
        rig.browse.koin.sourcePreferences.sourceDisplayMode.get().toString() shouldBe "List"
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
    }
}
