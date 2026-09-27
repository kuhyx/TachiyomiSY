package eu.kanade.tachiyomi.ui.manga

import android.view.View
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.ui.base.poll
import eu.kanade.tachiyomi.ui.base.pollLabel
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import tachiyomi.domain.track.interactor.GetTracks

/** The Compose dialog around the edit form: saving, cancelling, and picking a tracker to autofill from. */
@RunWith(RobolectricTestRunner::class)
internal class EditMangaDialogTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rig = EditMangaRig()
    private val getTracks = mockk<GetTracks>()
    private val first = logo("First")
    private val second = logo("Second")
    private val trackers = mockk<TrackerManager> {
        every { get(1L) } returns first
        every { get(2L) } returns second
    }
    private val events = mutableListOf<Any?>()

    private fun logo(label: String) = mockk<BaseTracker>(relaxed = true) {
        every { name } returns label
        every { getLogo() } returns R.drawable.brand_anilist
    }

    @Before
    fun setUp() {
        rig.start(module { single { getTracks } }, module { single { trackers } })
        coEvery { getTracks.await(1L) } returns listOf(domainTrack(1L), domainTrack(2L))
        coEvery { second.getMangaMetadata(any()) } returns TrackMangaMetadata(title = "From second")
        compose.activity.setTheme(R.style.Theme_Tachiyomi)
        compose.setContent {
            MaterialTheme {
                EditMangaDialog(
                    manga = rig.edited(null),
                    onDismissRequest = { events += "dismiss" },
                    onPositiveClick = { title, _, _, _, _, tags, status -> events.add(listOf(title, tags, status)) },
                )
            }
        }
        compose.pollLabel("Save")
    }

    @After
    fun tearDown() = rig.stop()

    private fun form(id: Int): View = ShadowDialog.getLatestDialog().findViewById(id)

    @Test
    fun saveSubmitsAndCloses() {
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        events shouldBe listOf(listOf("", listOf("one"), null), "dismiss")
    }

    @Test
    fun cancelOnlyCloses() {
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        events shouldBe listOf("dismiss")
    }

    @Test
    fun resetButtonsClearTheForm() {
        (form(R.id.title) as EditText).setText("typed")
        form(R.id.reset_info).performClick()
        form(R.id.reset_tags).performClick()
        (form(R.id.title) as EditText).text.toString() shouldBe ""
    }

    @Test
    fun trackerIsPickedToAutofill() {
        val title = form(R.id.title) as EditText
        form(R.id.autofill_from_tracker).performClick()
        compose.pollLabel("Select a tracker")
        compose.onNodeWithContentDescription("Second").performClick()
        compose.poll({ "not filled" }) { title.text.toString() == "From second" }
        compose.onNodeWithText("Select a tracker").assertDoesNotExist()
    }

    @Test
    fun trackerPickerCancels() {
        form(R.id.autofill_from_tracker).performClick()
        compose.pollLabel("Select a tracker")
        compose.onAllNodesWithText("Cancel").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Select a tracker").assertDoesNotExist()
    }
}
