package eu.kanade.presentation.more.settings.screen.debug

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "h2000dp")
internal class DebugInfoScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val store = MapPreferenceStore()
    private val base = BasePreferences(ApplicationProvider.getApplicationContext(), store)
    private val navigator = mockk<Navigator>(relaxed = true)
    private val sdk = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { base }
                    single { UiPreferences(store) }
                },
            )
        }
        mockkObject(DeviceUtil)
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        unmockkAll()
        stopKoin()
    }

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalNavigator provides navigator) {
                MaterialTheme { DebugInfoScreen().Content() }
            }
        }
        compose.waitForIdle()
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun navigatesToSubScreens() {
        every { DeviceUtil.oneUiVersion } returns 6.1
        show()
        count("OneUI version") shouldBe 1
        compose.onNodeWithText("Worker info").performClick()
        verify { navigator.push(any<WorkerInfoScreen>()) }
        compose.onNodeWithText("Backup file schema").performClick()
        verify { navigator.push(any<BackupSchemaScreen>()) }
    }

    @Test
    fun installationIdRenewsAndCopies() {
        every { DeviceUtil.oneUiVersion } returns null
        every { DeviceUtil.miuiMajorVersion } returns 14
        base.installationId.set("old-id")
        show()
        count("MIUI version") shouldBe 1
        compose.onNodeWithText("old-id").performClick()
        compose.onNode(hasClickAction() and hasAnyAncestor(hasText("Installation ID"))).performClick()
        compose.waitForIdle()
        base.installationId.get() shouldNotBe "old-id"
    }

    @Test
    fun plainDeviceOnAndroid11() {
        every { DeviceUtil.oneUiVersion } returns null
        every { DeviceUtil.miuiMajorVersion } returns null
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.R)
        show()
        count("OneUI version") shouldBe 0
        count("MIUI version") shouldBe 0
    }

    @Test
    fun plainDeviceOnAndroid10() {
        every { DeviceUtil.oneUiVersion } returns null
        every { DeviceUtil.miuiMajorVersion } returns null
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.Q)
        show()
        count("Android version") shouldBe 1
    }
}
