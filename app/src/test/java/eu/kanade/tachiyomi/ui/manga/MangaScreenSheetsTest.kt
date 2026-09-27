package eu.kanade.tachiyomi.ui.manga

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** The chapter settings, tracking and cover sheets, and the two SY edit dialogs. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class MangaScreenSheetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = MangaDialogsRig(compose)

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun scanlatorFilterFromSettings() {
        rig.show(MangaScreenModel.Dialog.SettingsSheet) { it.copy(availableScanlators = setOf("Group")) }
        rig.await("Scanlator")
        rig.click("Scanlator")
        rig.await("Exclude scanlators")
        rig.click("OK")
        coVerify(timeout = 5_000) { rig.harness.parts.setExcludedScanlators.await(1L, any()) }
    }

    @Test
    fun scanlatorFilterCancels() {
        rig.show(MangaScreenModel.Dialog.SettingsSheet)
        rig.await("Scanlator")
        rig.click("Scanlator")
        rig.await("Exclude scanlators")
        rig.click("Cancel")
        compose.waitForIdle()
    }

    @Test
    fun trackingSheetOpens() {
        rig.show(MangaScreenModel.Dialog.TrackSheet)
        compose.waitForIdle()
        verify(timeout = 5_000) { rig.harness.trackerManager.loggedInTrackers() }
    }

    @Test
    fun coverSharesAndSaves() {
        rig.show(MangaScreenModel.Dialog.FullCover)
        rig.await("Share")
        rig.click("Share")
        rig.click("Save")
    }

    @Test
    fun coverIsReplaced() {
        rig.registry.answer = { Uri.fromFile(File.createTempFile("cover", ".jpg")) }
        rig.show(MangaScreenModel.Dialog.FullCover)
        rig.await("Edit cover")
        rig.click("Edit cover")
        rig.registry.launched.size shouldBeGreaterThanOrEqual 1
    }

    @Test
    fun customCoverCanBeDeleted() {
        every { rig.harness.parts.coverCache.getCustomCoverFile(any()) } returns File.createTempFile("custom", ".jpg")
        rig.show(MangaScreenModel.Dialog.FullCover)
        rig.await("Edit cover")
        rig.click("Edit cover")
        rig.await("Delete")
        rig.click("Delete")
        verify(timeout = 5_000) { rig.harness.parts.coverCache.deleteCustomCover(1L) }
        rig.click("Edit cover")
        rig.await("Edit")
        rig.registry.answer = { null }
        rig.click("Edit")
    }

    @Test
    fun infoEditorSaves() {
        rig.show(MangaScreenModel.Dialog.EditMangaInfo(manga(favorite = true)))
        rig.await("Save")
        rig.click("Save")
        coVerify(timeout = 5_000) { rig.harness.parts.setCustomMangaInfo.set(any()) }
    }

    @Test
    fun mergedSettingsSaveAndDismiss() {
        rig.show(MangaScreenModel.Dialog.EditMergedSettings(mergedData(manga().copy(id = 5L))))
        rig.await("Save")
        rig.click("Save")
        rig.model.awaitSuccess { it.dialog == null }
    }
}
