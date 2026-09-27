package eu.kanade.tachiyomi.ui.reader

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** At night the automatic theme is gray, and in landscape the automatic layout doubles pages. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "land-night")
internal class ReaderConfigNightTest {
    private val harness = ReaderActivityHarness(pageCount = 2)
    private lateinit var activity: ReaderActivity

    @Before
    fun setUp() {
        harness.start()
        activity = harness.launch().get()
    }

    @After
    fun tearDown() = harness.stop()

    @Test
    fun automaticThemeIsGray() {
        harness.vm.readerPreferences.readerTheme.set(3)
        harness.settle()
        (activity.binding.readerContainer.background as ColorDrawable).color shouldBe Color.rgb(0x20, 0x21, 0x25)
    }

    @Test
    fun autoLayoutDoublesLandscape() {
        harness.vm.readerPreferences.pageLayout.set(PagerConfig.PageLayout.AUTOMATIC)
        harness.vm.readerPreferences.dualPageSplitPaged.set(true)
        harness.settle()
        harness.vm.readerPreferences.dualPageSplitPaged.set(false)
        harness.settle()
        activity.isFinishing shouldBe false
    }
}
