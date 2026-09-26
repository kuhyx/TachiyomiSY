package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.library.startNow
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.library.model.LibraryGroup

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun overflow(item: String) = rig.overflow(item)

    @Test
    fun openingAnEntry() {
        rig.show()
        rig.click("Alpha")
        rig.waitFor("opened:MangaScreen")
    }

    @Test
    fun emptyLibraryLinksTheGuide() {
        rig.harness.library.value = emptyList()
        rig.show("Your library is empty")
        rig.click("Getting started guide")
        shadowOf(compose.activity).nextStartedActivity.dataString shouldBe
            "https://mihon.app/docs/guides/getting-started"
    }

    @Test
    fun updatingTheCategory() {
        rig.show()
        overflow("Update category")
        rig.waitFor("Updating category")
        verify { LibraryUpdateJob.startNow(any<Context>(), rig.reading, any(), LibraryGroup.BY_DEFAULT, null) }
    }

    @Test
    fun updatingTheLibrary() {
        rig.show()
        overflow("Update library")
        rig.waitFor("Updating library")
        // The snackbar times out on its own.
        rig.waitUntilGone("Updating library", SNACKBAR_WAIT)
        every { LibraryUpdateJob.startNow(any<Context>(), null, any(), any(), any()) } returns false
        overflow("Update library")
        rig.waitFor("An update is already running")
    }

    @Test
    fun groupedUpdatesNameTheGroup() {
        rig.show()
        listOf(LibraryGroup.BY_SOURCE, LibraryGroup.BY_TRACK_STATUS, LibraryGroup.BY_STATUS).forEach { group ->
            rig.harness.libraryPreferences.groupLibraryBy.set(group)
            rig.updateUntil { LibraryUpdateJob.startNow(any<Context>(), null, any(), group, any<String>()) }
            // A global update names no group entry.
            overflow("Update library")
            verify { LibraryUpdateJob.startNow(any<Context>(), null, any(), group, null) }
        }
        rig.harness.libraryPreferences.groupLibraryBy.set(LibraryGroup.UNGROUPED)
        rig.updateUntil { LibraryUpdateJob.startNow(any<Context>(), null, any(), LibraryGroup.UNGROUPED, null) }
    }

    @Test
    fun randomEntries() {
        rig.show()
        overflow("Open random entry")
        rig.waitFor("opened:MangaScreen")
    }

    @Test
    fun noRandomEntryInAnEmptyCategory() {
        rig.harness.library.value = emptyList()
        rig.show("Your library is empty")
        overflow("Open random entry")
        rig.waitFor("No entries found in this category")
    }

    @Test
    fun syncingTheLibrary() {
        rig.harness.syncPreferences.syncService.set(1)
        rig.show()
        overflow("Sync library")
        verify { SyncDataJob.startNow(any(), manual = true) }
        every { SyncDataJob.isRunning(any()) } returns true
        overflow("Sync library")
        ShadowToast.getTextOfLatestToast() shouldBe "Sync is already in progress"
    }

    @Test
    fun searchFromElsewhere() {
        rig.show()
        CoroutineScope(Dispatchers.IO).launch { LibraryTab.search("zzz") }
        rig.waitFor("zzz")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(LIBRARY_WAIT) {
            compose.onAllNodes(hasText("zzz"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }

    private companion object {
        const val SNACKBAR_WAIT = 15_000L
    }
}
