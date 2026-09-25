package eu.kanade.presentation.library.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import exh.favorites.FavoritesSyncStatus
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SyncFavoritesDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun confirmDialogButtons() {
        compose.setContent {
            MaterialTheme {
                SyncFavoritesConfirmDialog(onDismissRequest = { events += "dismiss" }, onAccept = { events += "ok" })
            }
        }
        compose.onNodeWithText("E-Hentai Favorites sync").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("ok", "dismiss")
    }

    @Test
    fun warningDialogAccepts() {
        compose.setContent {
            MaterialTheme {
                SyncFavoritesWarningDialog(onDismissRequest = {}, onAccept = { events += "ok" })
            }
        }
        compose.onNodeWithText("IMPORTANT FAVORITES SYNC NOTES").assertExists()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("ok")
    }

    @Test
    fun progressDialogFollowsTheStatus() {
        var status: FavoritesSyncStatus by mutableStateOf(FavoritesSyncStatus.Idle)
        compose.setContent {
            MaterialTheme {
                SyncFavoritesProgressDialog(
                    status = status,
                    setStatusIdle = { events += "idle" },
                    openManga = { events += "open $it" },
                )
            }
        }
        compose.onNodeWithText("Favorites syncing").assertDoesNotExist()
        status = FavoritesSyncStatus.Initializing
        compose.waitForIdle()
        compose.onNodeWithText("Initializing sync").assertExists()
        status = FavoritesSyncStatus.Processing.CleaningUp
        compose.waitForIdle()
        status = FavoritesSyncStatus.BadLibraryState.MangaInMultipleCategories(3L, "Title", listOf("a"))
        compose.waitForIdle()
        compose.onNodeWithText("Show Gallery").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("open 3", "idle", "idle")
    }

    @Test
    fun slowGalleriesAreNamedAfterADelay() {
        compose.setContent {
            MaterialTheme {
                SyncFavoritesProgressDialog(
                    status = FavoritesSyncStatus.Processing.AddingGalleryToLocal(1, 2, false, "Slow one"),
                    setStatusIdle = {},
                    openManga = {},
                )
            }
        }
        compose.onNodeWithText("Slow one", substring = true).assertDoesNotExist()
        compose.mainClock.advanceTimeBy(6_000L)
        compose.waitForIdle()
        compose.onNodeWithText("Slow one", substring = true).assertExists()
    }
}
