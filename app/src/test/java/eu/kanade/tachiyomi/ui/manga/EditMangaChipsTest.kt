package eu.kanade.tachiyomi.ui.manga

import android.content.DialogInterface
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.google.android.material.chip.Chip
import eu.kanade.tachiyomi.R
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** The tag chips: removing one, adding comma-separated ones, and reading them back. */
@RunWith(RobolectricTestRunner::class)
internal class EditMangaChipsTest {
    private val rig = EditMangaRig()
    private val group get() = rig.binding.mangaGenresTags

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    private fun addTags(input: String?, button: Int = DialogInterface.BUTTON_POSITIVE) {
        (group.children.last() as Chip).performClick()
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        ShadowLooper.idleMainLooper()
        if (input != null) dialog.findViewById<EditText>(R.id.text_input)!!.setText(input)
        dialog.getButton(button).performClick()
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun chipsListTheirTags() {
        group.setChips(listOf("a", "b"), rig.scope)
        group.childCount shouldBe 3
        group.getTextStrings() shouldBe listOf("a", "b")
    }

    @Test
    fun closeIconRemovesChip() {
        group.setChips(listOf("a", "b"), rig.scope)
        (group.getChildAt(0) as Chip).performCloseIconClick()
        group.getTextStrings() shouldBe listOf("b")
    }

    @Test
    fun addedTagsAreSplit() {
        group.setChips(listOf("a"), rig.scope)
        addTags("c, d , ,")
        group.getTextStrings() shouldBe listOf("a", "c", "d")
    }

    @Test
    fun blankOrCancelledAddsNothing() {
        group.setChips(listOf("a"), rig.scope)
        addTags(null)
        addTags("  ")
        addTags("x", DialogInterface.BUTTON_NEGATIVE)
        group.getTextStrings() shouldBe listOf("a")
    }
}
