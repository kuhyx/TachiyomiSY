package eu.kanade.tachiyomi.util.view

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.viewinterop.AndroidView
import eu.kanade.domain.ui.UiPreferences
import io.kotest.matchers.shouldBe
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

/** The two helpers that host Compose content inside the app's theme. */
@RunWith(RobolectricTestRunner::class)
internal class ViewComposeContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun setUp() {
        startKoin { modules(module { single { UiPreferences(InMemoryPreferenceStore()) } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun aComposeViewGetsTheAppTheme() {
        compose.setContent {
            AndroidView(
                factory = { context ->
                    ComposeView(context).apply { setComposeContent { Text("hosted") } }
                },
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText("hosted").assertExists()
    }

    // The activity helper is inline, so only its compiled copy is attributable to the file.
    @Test
    fun theActivityHelperHostsContent() {
        val method = Class.forName("eu.kanade.tachiyomi.util.view.ViewExtensionsKt").declaredMethods
            .single { it.name == "setComposeContent" && it.parameterTypes[0] == ComponentActivity::class.java }
        method.isAccessible = true
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        val content: (Composer, Int) -> Unit = { _, _ -> }
        method.invoke(null, activity, null, content)
        val withDefault = Class.forName("eu.kanade.tachiyomi.util.view.ViewExtensionsKt").declaredMethods
            .single { it.name == "setComposeContent\u0024default" }
        withDefault.isAccessible = true
        withDefault.invoke(null, activity, null, content, 1, null)
        activity.window.decorView.isAttachedToWindow shouldBe true
        controller.close()
    }
}
