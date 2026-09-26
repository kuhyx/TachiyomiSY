package eu.kanade.tachiyomi.ui.reader

import android.net.Uri
import android.view.KeyEvent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/** Android 9 with a right-to-left layout: the pre-Q and pre-U transition paths and RTL page texts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
internal class ReaderActivityLegacyTest {

    // Compose runs on a test clock: an animating page spinner never lets an auto-advancing clock idle.
    @get:Rule
    val compose = createEmptyComposeRule()

    private val harness = ReaderActivityHarness()

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        harness.start()
    }

    @After
    fun tearDown() {
        harness.stop()
    }

    @Test
    fun oldAndroidOpensAndCloses() {
        val controller = harness.launch()
        val activity = controller.get()
        val pages = activity.viewModel.state.value.currentChapter!!.pages!!
        activity.resources.configuration.setLayoutDirection(Locale.forLanguageTag("ar"))
        activity.onPageSelected(pages[1], hasExtraPage = true)
        activity.viewModel.state.value.currentPageText shouldBe "3-2"
        activity.onShareImageResult(Uri.parse("content://reader/1"), pages[0], pages[1])
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)) shouldBe true
        activity.finish()
        activity.isFinishing shouldBe true
    }

    @Test
    fun recreatedKeepsViewModel() {
        val controller = harness.launch()
        controller.recreate()
        harness.settle()
        val activity = controller.get()
        activity.viewModel.needsInit() shouldBe false
        activity.viewModel.onViewerLoaded(null)
        controller.pause().stop().destroy()
    }
}
