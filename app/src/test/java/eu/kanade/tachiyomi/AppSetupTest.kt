package eu.kanade.tachiyomi

import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.util.system.GLUtil
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import mihon.core.firebase.FirebaseConfig
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil

@RunWith(RobolectricTestRunner::class)
internal class AppSetupTest {
    private val store = MapPreferenceStore()
    private val app = attachedApp()
    private val base = BasePreferences(app, store)
    private val privacy = PrivacyPreferences(store)
    private val sync = SyncPreferences(store)
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { base }
                    single { privacy }
                    single { sync }
                },
            )
        }
        mockkObject(FirebaseConfig, GLUtil, SyncDataJob.Companion)
        every { GLUtil.DEVICE_TEXTURE_LIMIT } returns 4096
        every { SyncDataJob.startNow(any(), any()) } just runs
    }

    @After
    fun tearDown() {
        scope.cancel()
        app.disableIncognitoReceiver.unregister()
        stopKoin()
        unmockkAll()
    }

    @Test
    fun incognitoFollowsThePreference() {
        app.observeIncognitoMode(scope)
        base.incognitoMode.set(true)
        base.incognitoMode.set(false)
        base.incognitoMode.get() shouldBe false
    }

    @Test
    fun firebaseFollowsPrivacy() {
        app.observeRuntimePreferences(scope)
        privacy.analytics.set(false)
        privacy.crashlytics.set(false)
        verify { FirebaseConfig.setAnalyticsEnabled(false) }
        verify { FirebaseConfig.setCrashlyticsEnabled(false) }
    }

    @Test
    fun thresholdDefaultsToTheGpu() {
        app.observeRuntimePreferences(scope)
        base.hardwareBitmapThreshold.get() shouldBe 4096
        ImageUtil.hardwareBitmapThreshold shouldBe 4096
    }

    @Test
    fun setThresholdIsKept() {
        base.hardwareBitmapThreshold.set(2048)
        app.observeRuntimePreferences(scope)
        ImageUtil.hardwareBitmapThreshold shouldBe 2048
        verify(exactly = 0) { GLUtil.DEVICE_TEXTURE_LIMIT }
    }

    @Test
    fun syncStartsOnlyWhenAsked() {
        val onStart = store.getBoolean("sync_on_app_start", false)
        onStart.set(true)
        app.startSyncIfEnabledOnAppStart()
        sync.syncService.set(1)
        onStart.set(false)
        app.startSyncIfEnabledOnAppStart()
        verify(exactly = 0) { SyncDataJob.startNow(any(), any()) }
        onStart.set(true)
        app.startSyncIfEnabledOnAppStart()
        verify(exactly = 1) { SyncDataJob.startNow(app, any()) }
    }
}
