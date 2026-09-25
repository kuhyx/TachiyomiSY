package eu.kanade.tachiyomi.ui.history

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
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.base.TabHost
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.domain.migration.usecases.MigrateMangaUseCase
import org.koin.core.context.startKoin
import org.koin.dsl.module
import tachiyomi.domain.manga.model.Manga
import java.util.Date

internal typealias HistoryCompose = AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

/** The history tab over [HistoryHarness], with one entry read just now and what its dialogs pull from Injekt. */
internal class HistoryTabRig(private val compose: HistoryCompose) {
    val harness = HistoryHarness()
    val manga: Manga = Manga.create().copy(id = 1L, ogTitle = "Title 1", source = 3L)
    private val store = MapPreferenceStore()

    fun start() {
        startKoin {
            modules(
                harness.koinModules() + module {
                    single { UiPreferences(store) }
                    single { SourcePreferences(store) }
                    single { mockk<CoverCache>(relaxed = true) }
                    single { mockk<DownloadManager>(relaxed = true) }
                    single { mockk<MigrateMangaUseCase>(relaxed = true) }
                },
            )
        }
        every { harness.getHistory.subscribe(any()) } returns MutableStateFlow(listOf(history(1, Date())))
        coEvery { harness.getManga.await(1L) } returns manga
        coEvery { harness.getDuplicate(any()) } returns emptyList()
        coEvery { harness.getCategories.await() } returns emptyList()
        coEvery { harness.getCategories.await(any()) } returns emptyList()
        coEvery { harness.updateManga.awaitUpdateFavorite(any(), any()) } returns true
        coEvery { harness.getNextChapters.await(any(), any(), any()) } returns emptyList()
        coEvery { harness.getNextChapters.await(onlyUnread = false) } returns emptyList()
        every { harness.source.id } returns 3L
    }

    fun show() {
        compose.setContent { TabHost(HistoryTab) }
        waitFor("Title 1")
    }

    fun node(label: String) = compose.onNode(hasText(label) or hasContentDescription(label), useUnmergedTree = true)

    /**
     * Clicks [label] through its clickable's semantics action, which also reaches buttons laid out below the small
     * test window; when the label also titles a dialog, the button is the last match.
     */
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

    fun waitFor(text: String) {
        compose.waitUntil(TAB_WAIT) {
            compose.onAllNodes(hasText(text) or hasContentDescription(text), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}

internal const val TAB_WAIT = 5_000L
