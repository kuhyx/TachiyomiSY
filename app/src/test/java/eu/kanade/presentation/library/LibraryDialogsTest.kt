package eu.kanade.presentation.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibrarySort

@RunWith(RobolectricTestRunner::class)
internal class LibraryDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun showDelete(local: Boolean) {
        compose.setContent {
            MaterialTheme {
                DeleteLibraryMangaDialog(
                    containsLocalManga = local,
                    onDismissRequest = { events += "dismiss" },
                    onConfirm = { library, downloads -> events += "confirm $library $downloads" },
                )
            }
        }
    }

    @Test
    fun deleteNeedsAChoice() {
        showDelete(local = false)
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNodeWithText("Downloaded chapters").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("dismiss", "confirm false true")
    }

    @Test
    fun localMangaHasNoDownloadsOption() {
        showDelete(local = true)
        compose.onNodeWithText("Downloaded chapters").assertDoesNotExist()
        compose.onNodeWithText("From library").performClick()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
        events shouldContainExactly listOf("dismiss", "confirm true false")
    }

    @Test
    fun cancelDismisses() {
        showDelete(local = false)
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun sortOptionsDependOnFeatures() {
        sortOptions(hasTrackers = false, hasSortTags = false).map { it.second }.size shouldBe 9
        val all = sortOptions(hasTrackers = true, hasSortTags = true).map { it.second }
        all.size shouldBe 11
        (LibrarySort.Type.TrackerMean in all) shouldBe true
        (LibrarySort.Type.TagList in all) shouldBe true
    }

    @Test
    fun directionFlipsOnlyWhenToggling() {
        nextDirection(isTogglingDirection = true, sortDescending = true) shouldBe LibrarySort.Direction.Ascending
        nextDirection(isTogglingDirection = true, sortDescending = false) shouldBe LibrarySort.Direction.Descending
        nextDirection(isTogglingDirection = false, sortDescending = true) shouldBe LibrarySort.Direction.Descending
        nextDirection(isTogglingDirection = false, sortDescending = false) shouldBe LibrarySort.Direction.Ascending
    }

    @Test
    fun groupModesAreData() {
        GroupMode(1, tachiyomi.i18n.MR.strings.label_default, 0).copy(int = 2).int shouldBe 2
    }
}
