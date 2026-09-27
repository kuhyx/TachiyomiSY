package eu.kanade.tachiyomi.ui.more

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import eu.kanade.tachiyomi.ui.browse.ScreenHost
import eu.kanade.tachiyomi.ui.manga.track.BlankScreen
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class NewUpdateScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val host = ScreenHost(
        NewUpdateScreen(
            versionName = "v2",
            changelogInfo = """
                Fixes
                ---
                Checksums
                abc
            """.trimIndent(),
            releaseLink = "https://example.org/release",
            downloadLink = "https://example.org/app.apk",
        ),
    )

    @Before
    fun setUp() {
        mockkObject(AppUpdateDownloadJob.Companion)
        every { AppUpdateDownloadJob.start(any(), any(), any()) } just runs
        host.show(compose)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun checksumsAreHidden() {
        fun count(text: String) = compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size
        count("Checksums") shouldBe 0
        count("Fixes") shouldBe 1
    }

    @Test
    fun openShowsRelease() {
        tap("Open on GitHub")
        val app = ApplicationProvider.getApplicationContext<Application>()
        val started = shadowOf(app).nextStartedActivity
        started.action shouldBe Intent.ACTION_VIEW
        started.dataString shouldBe "https://example.org/release"
    }

    @Test
    fun notNowPops() {
        tap("Not now")
        host.top.shouldBeInstanceOf<BlankScreen>()
    }

    @Test
    fun downloadStartsJob() {
        tap("Download")
        verify { AppUpdateDownloadJob.start(any(), "https://example.org/app.apk", "v2") }
        host.top.shouldBeInstanceOf<BlankScreen>()
    }
}
