package exh.ui.login

import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import exh.source.ExhPreferences
import exh.ui.baseActivityModule
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
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers
import tachiyomi.core.common.preference.InMemoryPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class EhLoginActivityLaunchTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val sdk = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        stopKoin()
        startKoin {
            modules(baseActivityModule(context), module { single { ExhPreferences(InMemoryPreferenceStore()) } })
        }
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        stopKoin()
    }

    @Test
    fun withoutWebViewItFinishes() {
        val activity = Robolectric.buildActivity(EhLoginActivity::class.java).setup().get()
        ShadowToast.getTextOfLatestToast() shouldBe "WebView is required for the app to function"
        activity.isFinishing shouldBe true
    }

    @Test
    fun olderAndroidTransitions() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.TIRAMISU)
        val activity = Robolectric.buildActivity(EhLoginActivity::class.java).setup().get()
        activity.isFinishing shouldBe true
    }

    @Test
    fun theIntentClearsTheTop() {
        val intent = EhLoginActivity.newIntent(context)
        intent.component?.className shouldBe EhLoginActivity::class.java.name
        (intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) shouldBe Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}
