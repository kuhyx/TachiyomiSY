package eu.kanade.tachiyomi.ui.category.biometric

import android.os.Looper
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.android.material.timepicker.MaterialTimePicker
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import eu.kanade.tachiyomi.ui.base.ScreenHost
import eu.kanade.tachiyomi.ui.base.delegate.SecureTestActivity
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import com.google.android.material.R as MaterialR

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
internal class BiometricTimesScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val activityKoin = ActivityKoin()
    private lateinit var activity: SecureTestActivity

    @Before
    fun setUp() {
        startKoin { modules(activityKoin.module()) }
        activityKoin.security.authenticatorTimeRanges.set(setOf("60,120"))
        activity = Robolectric.buildActivity(SecureTestActivity::class.java).setup().get()
        activity.setContent { ScreenHost(BiometricTimesScreen()) }
        compose.waitUntil(WAIT) { compose.onAllNodesWithContentDescription("").fetchSemanticsNodes().isNotEmpty() }
    }

    @After
    fun tearDown() = stopKoin()

    private fun picker(): MaterialTimePicker {
        shadowOf(Looper.getMainLooper()).idle()
        return activity.supportFragmentManager.fragments.filterIsInstance<MaterialTimePicker>().last()
    }

    private fun MaterialTimePicker.confirm() {
        requireDialog().findViewById<View>(MaterialR.id.material_timepicker_ok_button).performClick()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun twoPickersCreateARange() {
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        picker().confirm()
        picker().confirm()
        compose.waitUntil(WAIT) { activityKoin.security.authenticatorTimeRanges.get().size == 2 }
        activityKoin.security.authenticatorTimeRanges.get() shouldBe setOf("60,120", "0,0")
    }

    @Test
    fun dismissingThePickerCloses() {
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        picker().dismiss()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        activityKoin.security.authenticatorTimeRanges.get() shouldBe setOf("60,120")
    }

    @Test
    fun conflictingRangeIsToasted() {
        activityKoin.security.authenticatorTimeRanges.set(setOf("0,120"))
        compose.waitUntil(WAIT) { compose.onAllNodesWithContentDescription("").fetchSemanticsNodes().size == 2 }
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        picker().confirm()
        picker().confirm()
        compose.waitUntil(WAIT) { ShadowToast.getTextOfLatestToast() != null }
        ShadowToast.getTextOfLatestToast() shouldBe "A lock time conflicts with one that already exists!"
    }

    @Test
    fun deletingConfirms() {
        compose.onAllNodesWithContentDescription("")[1].performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitUntil(WAIT) { activityKoin.security.authenticatorTimeRanges.get().isEmpty() }
        compose.onNodeWithContentDescription("Navigate up").performClick()
    }
}

private const val WAIT = 5_000L
