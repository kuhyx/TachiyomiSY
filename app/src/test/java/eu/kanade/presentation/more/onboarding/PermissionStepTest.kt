package eu.kanade.presentation.more.onboarding

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.kanade.presentation.more.settings.screen.FakeResultRegistry
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
internal class PermissionStepTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = OnboardingKoin()
    private val registry = FakeResultRegistry()
    private val sdk = Build.VERSION.SDK_INT
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        koin.start()
        registry.answer = { true }
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        stopKoin()
    }

    private fun show(step: PermissionStep = PermissionStep()) {
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides registry.owner()) {
                MaterialTheme { step.Content() }
            }
        }
        compose.waitForIdle()
    }

    private fun grants() = compose.onAllNodesWithText("Grant")

    @Test
    fun grantButtonsLaunch() {
        show()
        grants().fetchSemanticsNodes().size shouldBe 3
        grants()[0].performClick()
        grants()[1].performClick()
        grants()[2].performClick()
        compose.waitForIdle()
        registry.launched.size shouldBe 1
        shadowOf(context as Application).nextStartedActivity.action shouldBe
            "android.settings.MANAGE_UNKNOWN_APP_SOURCES"
    }

    @Test
    fun privacySwitchesStore() {
        show()
        val before = koin.privacy.crashlytics.get()
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.onAllNodes(isToggleable()).onLast().performClick()
        compose.waitForIdle()
        koin.privacy.crashlytics.get() shouldBe !before
    }

    @Test
    fun batteryAlreadyGranted() {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowOf(power).setIgnoringBatteryOptimizations(context.packageName, true)
        val step = PermissionStep()
        show(step)
        step.batteryGranted shouldBe true
        grants().fetchSemanticsNodes().size shouldBe 2
        step.isComplete shouldBe true
    }

    @Test
    fun olderAndroidSkipsNotifications() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S_V2)
        val step = PermissionStep()
        show(step)
        step.notificationGranted shouldBe true
        compose.onAllNodesWithText("Notification permission").fetchSemanticsNodes().size shouldBe 0
    }
}
