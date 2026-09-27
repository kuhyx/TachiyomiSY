package exh.md

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.online.all.MangaDex
import eu.kanade.tachiyomi.ui.main.MainActivity
import exh.md.utils.MdUtil
import exh.md.utils.getEnabledMangaDex
import exh.ui.baseActivityModule
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.source.service.SourceManager

private const val WAIT_MS = 20_000L

@RunWith(RobolectricTestRunner::class)
internal class MangaDexLoginActivityTest {
    private val mangaDex = mockk<MangaDex>(relaxed = true)
    private var enabled: MangaDex? = mangaDex

    @Before
    fun setUp() {
        // The loading spinner's infinite animation keeps an unpaused Choreographer from ever idling.
        ShadowChoreographer.setPaused(true)
        val app = ApplicationProvider.getApplicationContext<Application>()
        val sourceManager = mockk<SourceManager> { every { isInitialized } returns MutableStateFlow(true) }
        mockkStatic("exh.md.utils.MdSourcesKt")
        every { MdUtil.getEnabledMangaDex(any(), any()) } answers { enabled }
        stopKoin()
        startKoin {
            modules(
                baseActivityModule(app),
                module {
                    single { sourceManager }
                    single { SourcePreferences(InMemoryPreferenceStore()) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        ShadowChoreographer.setPaused(false)
        unmockkAll()
        stopKoin()
    }

    private fun launch(uri: String?): MangaDexLoginActivity {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MangaDexLoginActivity::class.java)
        uri?.let { intent.data = it.toUri() }
        val activity = Robolectric.buildActivity(MangaDexLoginActivity::class.java, intent).setup().get()
        val deadline = System.currentTimeMillis() + WAIT_MS
        // The login runs on an IO thread that finishes the activity and only then starts the next one.
        val started = { shadowOf(activity).peekNextStartedActivity() != null }
        while (!(activity.isFinishing && started()) && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        activity.isFinishing shouldBe true
        shadowOf(activity).nextStartedActivity.component?.className shouldBe MainActivity::class.java.name
        return activity
    }

    @Test
    fun aCodeLogsIn() {
        launch("tachiyomisy://mangadex-auth?code=abc")
        coVerify { mangaDex.login("abc") }
    }

    @Test
    fun noCodeLogsOut() {
        launch("tachiyomisy://mangadex-auth?error=denied")
        coVerify { mangaDex.logout() }
    }

    @Test
    fun withoutMangaDexNothingHappens() {
        enabled = null
        launch("tachiyomisy://mangadex-auth?code=abc")
        launch("tachiyomisy://mangadex-auth")
        coVerify(exactly = 0) { mangaDex.login(any()) }
    }

    @Test
    fun noDataReturnsAtOnce() {
        launch(null)
    }
}
