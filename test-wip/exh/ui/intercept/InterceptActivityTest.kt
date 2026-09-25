package exh.ui.intercept

import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import exh.GALLERY_URL
import exh.GalleryAdderHarness
import exh.importableSource
import exh.ui.baseActivityModule
import io.kotest.matchers.shouldBe
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers
import tachiyomi.core.common.Constants

private const val WAIT_MS = 20_000L

@RunWith(RobolectricTestRunner::class)
internal class InterceptActivityTest {
    private val harness = GalleryAdderHarness()
    private val sdk = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        harness.start()
        loadKoinModules(baseActivityModule(harness.context))
        every { harness.sourceManager.isInitialized } returns MutableStateFlow(true)
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        harness.stop()
    }

    private fun launch(intent: Intent = viewIntent()): ActivityController<InterceptActivity> =
        Robolectric.buildActivity(InterceptActivity::class.java, intent).setup()

    private fun viewIntent() = Intent(Intent.ACTION_VIEW, GALLERY_URL.toUri())

    private fun waitFor(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + WAIT_MS
        while (!condition() && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        condition() shouldBe true
    }

    @Test
    fun aMangaLinkOpensTheLibraryEntry() {
        val activity = launch().get()
        waitFor { activity.isFinishing }
        val started = shadowOf(activity).nextStartedActivity
        started.component?.className shouldBe MainActivity::class.java.name
        started.action shouldBe Constants.SHORTCUT_MANGA
        started.getLongExtra(Constants.MANGA_EXTRA, 0L) shouldBe harness.manga.id
    }

    @Test
    fun aChapterLinkOpensTheReader() {
        every { harness.source.mapUrlToChapterUrl(any()) } returns "/s/1/2"
        val activity = launch().get()
        waitFor { activity.isFinishing }
        shadowOf(activity).nextStartedActivity.component?.className shouldBe ReaderActivity::class.java.name
    }

    @Test
    fun aFailureShowsADialog() {
        every { harness.source.matchesUri(any()) } returns false
        val activity = launch().get()
        waitFor { ShadowAlertDialog.getLatestAlertDialog() != null }
        activity.isFinishing shouldBe false
        ShadowAlertDialog.getLatestAlertDialog().dismiss()
        ShadowLooper.idleMainLooper()
        activity.isFinishing shouldBe true
    }

    @Test
    fun severalSourcesAskWhichOne() {
        val second = importableSource(sourceId = 3L)
        every { harness.sourceManager.getVisibleSources() } returns listOf(harness.source, second)
        val activity = launch().get()
        waitFor { ShadowAlertDialog.getLatestAlertDialog() != null }
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        shadowOf(dialog).clickOnItem(1)
        waitFor { activity.isFinishing }
    }

    @Test
    fun aSecondLoadIsIgnored() {
        val activity = launch().get()
        waitFor { activity.isFinishing }
        runBlocking { activity.loadGallery(GALLERY_URL) }
        shadowOf(activity).nextStartedActivity.component?.className shouldBe MainActivity::class.java.name
        shadowOf(activity).nextStartedActivity shouldBe null
    }

    @Test
    fun otherIntentsDoNothing() {
        val controller = launch(Intent())
        ShadowLooper.idleMainLooper()
        controller.get().isFinishing shouldBe false
        controller.pause().stop().start().resume()
        controller.get().finish()
        controller.get().isFinishing shouldBe true
    }

    @Test
    fun olderAndroidTransitions() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.TIRAMISU)
        val controller = launch(Intent())
        controller.get().finish()
        controller.get().isFinishing shouldBe true
    }
}
