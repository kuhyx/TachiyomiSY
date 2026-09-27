package eu.kanade.tachiyomi.ui.setting.track

import android.os.Handler
import android.os.Looper
import eu.kanade.tachiyomi.data.sync.service.GoogleDriveService
import eu.kanade.tachiyomi.ui.manga.eventually
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

/** The Google Drive callback hands the code to the service and reports its outcome, or the error it got. */
@RunWith(RobolectricTestRunner::class)
internal class GoogleDriveLoginActivityTest {
    private val drive = mockk<GoogleDriveService>()
    private val rig = LoginActivityRig(module { single { drive } })
    private var outcome: String? = null

    @Before
    fun setUp() {
        rig.start()
        // The real service answers on the main thread; so does this one.
        every { drive.handleAuthorizationCode(any(), any(), any(), any()) } answers {
            val success = thirdArg<() -> Unit>()
            val failure = arg<(String) -> Unit>(3)
            Handler(Looper.getMainLooper()).post { outcome?.let(failure) ?: success() }
        }
    }

    @After
    fun tearDown() = rig.stop()

    private fun open(uri: String?) = rig.launch(GoogleDriveLoginActivity::class.java, uri)

    private fun toasted(text: String) = eventually { ShadowToast.getTextOfLatestToast() == text }

    @Test
    fun codeLogsIn() {
        val activity = open("tachiyomi://google-drive-auth?code=abc")
        toasted("Logged in to Google Drive")
        rig.returned(activity)
        verify { drive.handleAuthorizationCode("abc", activity, any(), any()) }
    }

    @Test
    fun serviceFailureIsShown() {
        outcome = "denied"
        rig.returned(open("tachiyomi://google-drive-auth?code=abc"))
        toasted("Failed to log in to Google Drive: denied")
    }

    @Test
    fun errorParameterIsShown() {
        rig.returned(open("tachiyomi://google-drive-auth?error=nope"))
        ShadowToast.getTextOfLatestToast() shouldBe "Failed to log in to Google Drive: nope"
    }

    @Test
    fun neitherJustReturns() {
        rig.returned(open("tachiyomi://google-drive-auth?x=1"))
        verify(exactly = 0) { drive.handleAuthorizationCode(any(), any(), any(), any()) }
    }
}
