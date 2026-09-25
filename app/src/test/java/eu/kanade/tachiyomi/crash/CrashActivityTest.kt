package eu.kanade.tachiyomi.crash

import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.main.MainActivity
import io.kotest.matchers.nulls.shouldNotBeNull
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
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class CrashActivityTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setUp() {
        val store = MapPreferenceStore()
        val base = BasePreferences(ApplicationProvider.getApplicationContext(), store)
        startKoin {
            modules(
                module {
                    single { UiPreferences(store) }
                    single { SecurityPreferences(store) }
                    single { base }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun restartOpensMainActivity() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), CrashActivity::class.java)
            .putExtra("Throwable", "\"java.lang.IllegalStateException: boom\"")
        val activity = Robolectric.buildActivity(CrashActivity::class.java, intent).setup().get()
        compose.waitForIdle()
        compose.onNodeWithText("Restart the application").performClick()
        compose.waitForIdle()
        activity.isFinishing shouldBe true
        val started = shadowOf(activity).nextStartedActivity.shouldNotBeNull()
        started.component?.className shouldBe MainActivity::class.java.name
    }
}
