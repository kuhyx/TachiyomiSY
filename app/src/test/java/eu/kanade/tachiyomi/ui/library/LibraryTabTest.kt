package eu.kanade.tachiyomi.ui.library

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
internal class LibraryTabTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = LibraryTabRig(compose)
    private val harness = rig.harness

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun withEntries() {
        harness.categories.value = listOf(libCategory(1L))
        harness.libraryManga.value = listOf(libEntry(libManga(1L)))
    }

    @Test
    fun emptyLibraryLinksTheGuide() {
        rig.show()
        compose.waitForLabel("Your library is empty")
        rig.click("Getting started guide")
        shadowOf(compose.activity).nextStartedActivity.action shouldBe Intent.ACTION_VIEW
    }

    @Test
    fun loadingLibraryIsNotEmpty() {
        every { harness.getLibraryManga.subscribe() } returns flow { awaitCancellation() }
        rig.show()
        compose.waitForIdle()
        compose.hasLabel("Your library is empty") shouldBe false
    }

    @Test
    fun entriesOpenTheirManga() {
        withEntries()
        rig.show()
        compose.waitForLabel("Manga 1")
        rig.click("Manga 1")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun refreshReportsTheOutcome() {
        withEntries()
        rig.show()
        compose.waitForLabel("Manga 1")
        rig.action("Update library")
        compose.waitForLabel("An update is already running")
        rig.updateStarts = true
        rig.action("Update category")
        compose.waitForLabel("Updating category")
        rig.action("Update library")
        compose.waitForLabel("Updating library")
        rig.updates.map { it.category?.id } shouldBe listOf(null, 1L, null)
        rig.updates.map { it.groupExtra } shouldBe listOf(null, null, null)
    }

    @Test
    fun randomEntryNeedsEntries() {
        rig.show()
        compose.waitForLabel("Your library is empty")
        rig.action("Open random entry")
        compose.waitForLabel("No entries found in this category")
        withEntries()
        compose.waitForLabel("Manga 1")
        rig.action("Open random entry")
        compose.waitForLabel("opened:MangaScreen")
    }

    @Test
    fun syncLibraryUnlessRunning() {
        harness.syncPreferences.syncService.set(1)
        rig.show()
        rig.action("Sync library")
        verify { SyncDataJob.startNow(any(), manual = true) }
        every { SyncDataJob.isRunning(any()) } returns true
        rig.action("Sync library")
        ShadowToast.getTextOfLatestToast() shouldBe "Sync is already in progress"
    }

    @Test
    fun syncFavoritesWarnsFirst() {
        rig.show()
        rig.action("Sync EH favorites")
        compose.waitForLabel("IMPORTANT FAVORITES SYNC NOTES")
    }

    @Test
    fun searchFromElsewhereAndBack() {
        withEntries()
        rig.show()
        compose.waitForLabel("Manga 1")
        val sender = CoroutineScope(Dispatchers.Default).launch { LibraryTab.search("needle") }
        try {
            compose.waitForLabel("needle")
        } finally {
            sender.cancel()
        }
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(WAIT) { !compose.hasLabel("needle") }
    }

    @Test
    fun reselectOpensSettings() {
        rig.show()
        val sender = CoroutineScope(Dispatchers.Default).launch { LibraryTab.onReselect(mockk<Navigator>()) }
        try {
            compose.waitForLabel("Display")
        } finally {
            sender.cancel()
        }
    }

    @Test
    fun backClearsTheSelection() {
        withEntries()
        rig.show()
        compose.waitForLabel("Manga 1")
        rig.node("Manga 1").performTouchInput { longClick() }
        compose.waitForLabel("Select all")
        compose.activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(WAIT) { !compose.hasLabel("Select all") }
    }
}
