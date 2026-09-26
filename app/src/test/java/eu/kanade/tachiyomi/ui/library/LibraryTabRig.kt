package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.rules.ActivityScenarioRule
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.ui.base.TabHost
import eu.kanade.tachiyomi.ui.base.libraryManga
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.MockKVerificationScope
import io.mockk.unmockkAll
import io.mockk.verify
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.model.Category

internal typealias LibraryCompose = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

/** The library tab over [LibraryHarness] with one favourite in one category; updates and syncs are stubbed. */
internal class LibraryTabRig(private val compose: LibraryCompose) {
    val harness = LibraryHarness()
    val reading = Category(id = 1, name = "Reading", order = 1, flags = 0)
    val setDisplayMode = mockk<SetDisplayMode>(relaxed = true)
    val setSortMode = mockk<SetSortModeForCategory>(relaxed = true)

    fun start() {
        startKoin {
            modules(
                harness.koinModules() + module {
                    single { UiPreferences(harness.store) }
                    single { setDisplayMode }
                    single { setSortMode }
                },
            )
        }
        mockkStatic("eu.kanade.tachiyomi.data.library.LibraryUpdateSchedulingKt")
        every { LibraryUpdateJob.startNow(any<Context>(), any(), any(), any(), any()) } returns true
        mockkObject(SyncDataJob.Companion)
        every { SyncDataJob.isRunning(any()) } returns false
        every { SyncDataJob.startNow(any(), any()) } returns Unit
        coEvery { harness.getIdsWithMetadata.await() } returns emptyList()
        coEvery { harness.getTracks.await() } returns emptyList()
        harness.categories.value = listOf(reading)
        harness.library.value = listOf(libraryManga(1, manga(1, "Alpha"), categories = listOf(1L)))
    }

    fun stop() {
        unmockkAll()
        stopKoin()
    }

    fun show(wait: String = "Alpha") {
        compose.setContent { TabHost(LibraryTab) }
        waitFor(wait)
    }

    fun node(label: String) = compose.onNode(hasText(label) or hasContentDescription(label), useUnmergedTree = true)

    /** Clicks [label] through its clickable's semantics action; when several match, the last. */
    fun click(label: String) {
        val matcher = (hasText(label) or hasContentDescription(label)) and hasClickAction()
        val clickable = compose.onAllNodes(matcher)
        if (clickable.fetchSemanticsNodes().isNotEmpty()) {
            clickable.onLast().performSemanticsAction(SemanticsActions.OnClick)
        } else {
            compose.onAllNodes(hasText(label) or hasContentDescription(label), useUnmergedTree = true).onLast()
                .performClick()
        }
        compose.waitForIdle()
    }

    fun overflow(item: String) {
        click("More options")
        click(item)
    }

    /** Asks for a category update until [call] is verified: a regrouping reaches the toolbar asynchronously. */
    fun updateUntil(call: MockKVerificationScope.() -> Unit) {
        compose.waitUntil(LIBRARY_WAIT) {
            overflow("Update category")
            runCatching { verify(verifyBlock = call) }.isSuccess
        }
    }

    fun waitFor(text: String) {
        compose.waitUntil(LIBRARY_WAIT) {
            compose.onAllNodes(hasText(text) or hasContentDescription(text), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}

internal const val LIBRARY_WAIT = 5_000L
