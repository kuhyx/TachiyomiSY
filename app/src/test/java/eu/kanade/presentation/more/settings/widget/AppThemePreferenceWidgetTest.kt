package eu.kanade.presentation.more.settings.widget

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class AppThemePreferenceWidgetTest {
    @get:Rule
    val compose = createComposeRule()

    private val picked = mutableListOf<AppTheme>()

    @Before
    fun setUp() {
        stopKoin()
        startKoin { modules(module { single { UiPreferences(InMemoryPreferenceStore()) } }) }
    }

    @After
    fun tearDown() {
        unmockkAll()
        stopKoin()
    }

    private fun show(dynamic: Boolean, activity: Activity?) {
        mockkStatic("eu.kanade.tachiyomi.util.system.DeviceUtilExtensionsKt")
        every { DeviceUtil.isDynamicColorAvailable } returns dynamic
        compose.setContent {
            val base = LocalContext.current
            CompositionLocalProvider(LocalContext provides (activity ?: base)) {
                MaterialTheme {
                    AppThemePreferenceWidget(value = AppTheme.DEFAULT, amoled = true, onItemClick = { picked += it })
                }
            }
        }
        compose.waitForIdle()
    }

    private fun texts(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun monetShownWhenDynamic() {
        show(dynamic = true, activity = null)
        texts("Dynamic") shouldBe 1
        compose.onAllNodesWithContentDescription("Selected").fetchSemanticsNodes().size shouldBe 1
    }

    @Test
    fun monetHiddenWithoutDynamic() {
        show(dynamic = false, activity = null)
        texts("Dynamic") shouldBe 0
        compose.onAllNodesWithContentDescription("Selected").onFirst().performClick()
        picked shouldBe listOf(AppTheme.DEFAULT)
    }

    @Test
    fun clickRecreatesActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        show(dynamic = false, activity = activity)
        compose.onAllNodesWithContentDescription("Selected").onFirst().performClick()
        picked shouldBe listOf(AppTheme.DEFAULT)
    }
}
