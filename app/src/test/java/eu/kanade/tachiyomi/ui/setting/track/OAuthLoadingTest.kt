package eu.kanade.tachiyomi.ui.setting.track

import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.base.ActivityKoin
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
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
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/** A callback activity that keeps the result to itself and stays open, so its loading screen composes. */
internal class StayingOAuthActivity : BaseOAuthLoginActivity() {
    var handled: Uri? = null

    override fun handleResult(uri: Uri) {
        handled = uri
    }
}

/** While a callback is handled, the base activity shows its loading screen. */
@RunWith(RobolectricTestRunner::class)
internal class OAuthLoadingTest {
    private var controller: ActivityController<StayingOAuthActivity>? = null

    @Before
    fun setUp() {
        // The spinner would animate forever on an unpaused choreographer.
        ShadowChoreographer.setPaused(true)
        stopKoin()
        startKoin { modules(ActivityKoin().module(), module { single<TrackerManager> { mockk(relaxed = true) } }) }
    }

    @After
    fun tearDown() {
        try {
            controller?.pause()?.stop()?.destroy()
            shadowOf(Looper.getMainLooper()).idle()
            clearAllMocks()
            unmockkAll()
            stopKoin()
        } finally {
            ShadowChoreographer.setPaused(false)
        }
    }

    @Test
    fun loadingShowsWhileHandled() {
        val intent = Intent(Intent.ACTION_VIEW, "tachiyomi://somewhere?code=1".toUri())
        val launched = Robolectric.buildActivity(StayingOAuthActivity::class.java, intent).setup()
        controller = launched
        // The choreographer is paused: advancing the clock is what hands Compose its first frames.
        repeat(FRAMES) { ShadowLooper.idleMainLooper(FRAME_MS, TimeUnit.MILLISECONDS) }
        val activity = launched.get()
        activity.handled shouldBe intent.data
        activity.isFinishing shouldBe false
        val content = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ComposeView
        content.hasComposition shouldBe true
    }
}

private const val FRAMES = 10
private const val FRAME_MS = 16L
