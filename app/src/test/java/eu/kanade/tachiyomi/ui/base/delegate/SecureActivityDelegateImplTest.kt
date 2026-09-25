package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Application
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences.SecureScreenMode
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.security.UnlockActivity
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class SecureActivityDelegateImplTest {
    private val store = MapPreferenceStore()
    private val security = SecurityPreferences(store)
    private val base = BasePreferences(ApplicationProvider.getApplicationContext<Application>(), store)
    private val ui = UiPreferences(store)
    private val biometric = mockk<BiometricManager>()

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { security }
                    single { base }
                    single { ui }
                },
            )
        }
        mockkStatic(BiometricManager::class)
        every { BiometricManager.from(any()) } returns biometric
        supported(true)
        SecureActivityDelegate.requireUnlock = true
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
        SecureActivityDelegate.requireUnlock = true
    }

    private fun supported(value: Boolean) {
        every { biometric.canAuthenticate(any()) } returns if (value) {
            BiometricManager.BIOMETRIC_SUCCESS
        } else {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        }
    }

    private fun launch(): SecureTestActivity = Robolectric.buildActivity(SecureTestActivity::class.java).setup().get()

    private fun SecureTestActivity.secure(): Boolean =
        window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0

    @Test
    fun secureScreenFollowsTheModes() {
        security.secureScreen.set(SecureScreenMode.ALWAYS)
        launch().secure() shouldBe true
        security.secureScreen.set(SecureScreenMode.NEVER)
        launch().secure() shouldBe false
        security.secureScreen.set(SecureScreenMode.INCOGNITO)
        launch().secure() shouldBe false
        base.incognitoMode.set(true)
        launch().secure() shouldBe true
    }

    @Test
    fun noLockStartsNothing() {
        val activity = launch()
        shadowOf(activity).nextStartedActivity.shouldBeNull()
    }

    @Test
    fun lockStartsTheUnlockScreen() {
        security.useAuthenticator.set(true)
        val activity = launch()
        shadowOf(activity).nextStartedActivity.component?.className shouldBe UnlockActivity::class.java.name
    }

    @Test
    @Config(sdk = [33])
    fun legacyLockStartsTheUnlock() {
        security.useAuthenticator.set(true)
        val activity = launch()
        shadowOf(activity).nextStartedActivity.component?.className shouldBe UnlockActivity::class.java.name
    }

    @Test
    fun unlockedAppStartsNothing() {
        security.useAuthenticator.set(true)
        SecureActivityDelegate.unlock()
        shadowOf(launch()).nextStartedActivity.shouldBeNull()
    }

    @Test
    fun unsupportedDeviceDropsTheLock() {
        security.useAuthenticator.set(true)
        supported(false)
        shadowOf(launch()).nextStartedActivity.shouldBeNull()
        security.useAuthenticator.get() shouldBe false
    }

    @Test
    fun amoledThemeIsApplied() {
        ui.appTheme.set(AppTheme.NORD)
        ui.themeDarkAmoled.set(true)
        launch().isFinishing shouldBe false
    }
}
