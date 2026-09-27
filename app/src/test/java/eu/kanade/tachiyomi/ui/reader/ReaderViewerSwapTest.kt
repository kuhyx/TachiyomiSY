package eu.kanade.tachiyomi.ui.reader

import android.transition.Transition
import android.view.View
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.transition.platform.MaterialContainerTransform
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.useAutoWebtoon
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Swapping the viewer: after a shared-element entry, without a manga, for webtoon-typed entries, and insets. */
@RunWith(RobolectricTestRunner::class)
internal class ReaderViewerSwapTest {
    private var harness = ReaderActivityHarness(pageCount = 2)

    @After
    fun tearDown() = harness.stop()

    private fun launch(flags: Long = 0L, genres: List<String>? = null): ReaderActivity {
        harness = ReaderActivityHarness(pageCount = 2, viewerFlags = flags)
        harness.start()
        if (genres != null) {
            val manga = harness.vm.manga.copy(ogGenre = genres, viewerFlags = flags)
            coEvery { harness.vm.getManga.await(10L) } returns manga
            // A source without a name: the entry's type comes from its genres alone.
            every { harness.vm.sourceManager.get(any()) } returns null
        }
        return harness.launch().get()
    }

    // The framework keeps a transition's listeners private; end them as the window would.
    private fun Transition.finish() {
        val field = Transition::class.java.getDeclaredField("mListeners")
        field.isAccessible = true
        val listeners = (field.get(this) as? List<*>).orEmpty().filterIsInstance<Transition.TransitionListener>()
        listeners.forEach { it.onTransitionEnd(this) }
    }

    @Test
    fun sharedEntryWaitsToRotate() {
        val activity = launch()
        val transition = MaterialContainerTransform()
        activity.window.sharedElementEnterTransition = transition
        activity.updateViewer()
        transition.finish()
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<Viewer>()
    }

    @Test
    fun noMangaUsesDefaults() {
        val activity = launch()
        activity.viewModel.updateState { it.copy(manga = null) }
        activity.updateViewer()
        activity.updateViewer()
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<Viewer>()
    }

    @Test
    fun pickedModeBeatsAutoWebtoon() {
        val activity = launch(ReadingMode.WEBTOON.flagValue.toLong(), genres = listOf("Webtoon"))
        harness.vm.readerPreferences.useAutoWebtoon.get() shouldBe true
        activity.viewModel.state.value.viewer.shouldBeInstanceOf<WebtoonViewer>()
    }

    @Test
    fun insetsWithoutWindowInsets() {
        val activity = launch()
        val view = View(activity)
        activity.applyInsetsPadding(view, null, fullscreen = false, drawUnderCutout = false)
        activity.applyInsetsPadding(view, null, fullscreen = true, drawUnderCutout = false)
        activity.applyInsetsPadding(view, WindowInsetsCompat.CONSUMED, fullscreen = true, drawUnderCutout = false)
        activity.applyInsetsPadding(view, WindowInsetsCompat.CONSUMED, fullscreen = true, drawUnderCutout = true)
        view.paddingTop shouldBe 0
    }
}
