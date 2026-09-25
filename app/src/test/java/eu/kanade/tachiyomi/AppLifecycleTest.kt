package eu.kanade.tachiyomi

import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.util.system.WebViewUtil
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.chromium.base.BuildInfo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppLifecycleTest {
    private val store = MapPreferenceStore()
    private val app = attachedApp()
    private val base = BasePreferences(app, store)
    private val sync = SyncPreferences(store)
    private val owner = ProcessLifecycleOwner.get()

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { base }
                    single { sync }
                    single { SecurityPreferences(store) }
                },
            )
        }
        mockkObject(SyncDataJob.Companion, WebViewUtil)
        every { SyncDataJob.startNow(any(), any()) } just runs
        every { WebViewUtil.spoofedPackageName(any()) } returns "spoofed"
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
    }

    private fun syncWith(enabled: Boolean, onResume: Boolean) {
        sync.syncService.set(if (enabled) 1 else 0)
        store.getBoolean("sync_on_app_resume", false).set(onResume)
    }

    @Test
    fun startSyncsOnlyWhenAsked() {
        syncWith(enabled = false, onResume = true)
        app.onStart(owner)
        syncWith(enabled = true, onResume = false)
        app.onStart(owner)
        verify(exactly = 0) { SyncDataJob.startNow(any(), any()) }
        syncWith(enabled = true, onResume = true)
        app.onStart(owner)
        verify(exactly = 1) { SyncDataJob.startNow(app, any()) }
        app.onStop(owner)
    }

    @Test
    fun receiverTurnsIncognitoOff() {
        base.incognitoMode.set(true)
        val receiver = app.disableIncognitoReceiver
        receiver.register()
        receiver.register()
        receiver.onReceive(app, Intent(ACTION_DISABLE_INCOGNITO_MODE))
        base.incognitoMode.get() shouldBe false
        receiver.unregister()
        receiver.unregister()
    }

    @Test
    fun chromiumGetsASpoofedName() {
        BuildInfo.getAll(app) shouldBe "spoofed"
        BuildInfo.other(app) shouldNotBe "spoofed"
        app.packageName shouldNotBe "spoofed"
    }

    @Test
    fun spoofFailureFallsBack() {
        every { WebViewUtil.spoofedPackageName(any()) } throws IllegalStateException("no webview")
        BuildInfo.getAll(app) shouldNotBe "spoofed"
    }
}
