package eu.kanade.tachiyomi.widget

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class TachiyomiTextInputEditTextTest {
    private val context: Context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Tachiyomi)
    private val preferences = BasePreferences(context, MapPreferenceStore())

    @Before
    fun setUp() {
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun learningOff(view: TachiyomiTextInputEditText): Boolean =
        view.imeOptions and EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

    @Test
    fun everyConstructorBuilds() {
        TachiyomiTextInputEditText(context).isEnabled shouldBe true
        TachiyomiTextInputEditText(context, null).isEnabled shouldBe true
        TachiyomiTextInputEditText(context, null, R.attr.editTextStyle).isEnabled shouldBe true
    }

    @Test
    fun attachedViewFollowsIncognito() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.setTheme(R.style.Theme_Tachiyomi)
        val root = FrameLayout(activity)
        activity.setContentView(root)
        val view = TachiyomiTextInputEditText(ContextThemeWrapper(activity, R.style.Theme_Tachiyomi))
        root.addView(view)
        shadowOf(Looper.getMainLooper()).idle()
        learningOff(view) shouldBe false

        preferences.incognitoMode.set(true)
        shadowOf(Looper.getMainLooper()).idle()
        learningOff(view) shouldBe true

        preferences.incognitoMode.set(false)
        shadowOf(Looper.getMainLooper()).idle()
        learningOff(view) shouldBe false

        root.removeView(view)
        preferences.incognitoMode.set(true)
        shadowOf(Looper.getMainLooper()).idle()
        learningOff(view) shouldBe false
    }
}
