package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.home.HomeScreen
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import tachiyomi.domain.category.model.Category

/** What [LibraryTab]'s refresh action asked [LibraryUpdateJob.startNow] for. */
internal data class UpdateRequest(val category: Category?, val group: Int, val groupExtra: String?)

/**
 * [LibraryTab] composed as the home screen hosts it, over a [LibraryHarness]; library updates and
 * sync jobs are stubbed, and every requested update is recorded in [updates].
 */
internal class LibraryTabRig(
    private val compose: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
) {
    val harness: LibraryHarness = LibraryHarness()
    val updates: MutableList<UpdateRequest> = mutableListOf()
    var updateStarts: Boolean = false

    fun start() {
        harness.start()
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        // The receiver (the companion) is the first argument of the mocked extension.
        every { LibraryUpdateJob.startNow(any<Context>(), any(), any(), any(), any()) } answers {
            updates += UpdateRequest(category = arg(2), group = arg(4), groupExtra = arg(5))
            updateStarts
        }
        mockkObject(SyncDataJob.Companion)
        every { SyncDataJob.isRunning(any()) } returns false
        every { SyncDataJob.startNow(any(), any()) } just runs
    }

    fun stop() {
        harness.stop()
        unmockkAll()
        HomeScreen.showBottomNavEvent.tryReceive()
    }

    fun show() {
        // The recommendation sheet inflates Material views, which need the app theme.
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        compose.setContent { TabHost(LibraryTab) }
        compose.waitForLabel("tab:Library")
    }

    fun node(label: String): SemanticsNodeInteraction =
        compose.onNode(hasText(label) or hasContentDescription(label), useUnmergedTree = true)

    fun click(label: String) {
        node(label).performClick()
        compose.waitForIdle()
    }

    /** Clicks an app-bar action, opening the overflow menu first when it is not on the bar. */
    fun action(label: String) {
        if (!compose.hasLabel(label)) click("More options")
        click(label)
    }
}
