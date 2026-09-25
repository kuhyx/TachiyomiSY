package eu.kanade.presentation.more.settings.screen

import android.view.View
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFit
import eu.kanade.tachiyomi.ui.reader.setting.dualPageSplitPaged
import eu.kanade.tachiyomi.ui.reader.setting.navigationModePager
import eu.kanade.tachiyomi.ui.reader.setting.pageLayout
import eu.kanade.tachiyomi.ui.reader.setting.readWithVolumeKeys
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val PAGED = "Paged"
private const val STRIP = "Long strip"

@RunWith(RobolectricTestRunner::class)
internal class SettingsReaderScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)
    private val reader: ReaderPreferences get() = koin.reader

    @Before
    fun setUp() {
        koin.start()
    }

    @After
    fun tearDown() {
        unmockkAll()
        koin.stop()
    }

    private fun enabled(title: String, group: String? = null): Boolean = harness.item(title, group).enabled

    private fun settle() = compose.waitForIdle()

    @Test
    fun cutoutNeedsFullscreenAndNotch() {
        mockkStatic("eu.kanade.tachiyomi.util.system.DisplayExtensionsKt")
        every { any<View>().hasDisplayCutout() } returns true
        reader.fullscreen.set(false)
        harness.show(SettingsReaderScreen)
        enabled("Show content in cutout area") shouldBe false
        reader.fullscreen.set(true)
        settle()
        enabled("Show content in cutout area") shouldBe true
    }

    @Test
    fun cutoutOffWithoutNotch() {
        harness.show(SettingsReaderScreen)
        enabled("Show content in cutout area") shouldBe false
    }

    @Test
    fun flashSettingsFollowSwitch() {
        harness.show(SettingsReaderScreen)
        enabled("Flash with") shouldBe false
        reader.flashOnPageChange.set(true)
        settle()
        enabled("Flash with") shouldBe true
        harness.slide("Flash duration", value = 3)
        reader.flashDurationMillis.get() shouldBe 3 * ReaderPreferences.MILLI_CONVERSION
        harness.slide("Flash every", value = 4)
        reader.flashPageInterval.get() shouldBe 4
    }

    @Test
    fun pagedEnabledFlags() {
        harness.show(SettingsReaderScreen)
        enabled("Invert tap zones", PAGED) shouldBe true
        enabled("Pan wide images", PAGED) shouldBe true
        enabled("Automatically zoom into wide images", PAGED) shouldBe true
        reader.navigationModePager.set(5)
        reader.imageScaleType.set(2)
        settle()
        enabled("Invert tap zones", PAGED) shouldBe false
        enabled("Pan wide images", PAGED) shouldBe false
        enabled("Automatically zoom into wide images", PAGED) shouldBe false
    }

    @Test
    fun splitAndRotateExclusive() {
        harness.show(SettingsReaderScreen)
        reader.dualPageRotateToFit.set(true)
        harness.switch("Split wide pages", value = true, group = PAGED) shouldBe true
        reader.dualPageRotateToFit.get() shouldBe false
        reader.dualPageSplitPaged.set(true)
        harness.switch("Rotate wide pages to fit", value = true, group = PAGED) shouldBe true
        reader.dualPageSplitPaged.get() shouldBe false
        settle()
        enabled("Invert split page placement", PAGED) shouldBe false
        reader.dualPageRotateToFit.set(true)
        settle()
        enabled("Flip orientation of rotated wide pages", PAGED) shouldBe true
    }

    @Test
    fun webtoonPaddingSlider() {
        harness.show(SettingsReaderScreen)
        harness.slide("Side padding", value = 10, group = STRIP)
        reader.webtoonSidePadding.get() shouldBe 10
    }

    @Test
    fun navigationFlags() {
        harness.show(SettingsReaderScreen)
        enabled("Invert volume keys") shouldBe false
        enabled("Place vertical navigator on the left side") shouldBe false
        reader.readWithVolumeKeys.set(true)
        reader.verticalNavigator.set(setOf(ReadingMode.WEBTOON))
        settle()
        enabled("Invert volume keys") shouldBe true
        enabled("Vertical navigator height") shouldBe true
        harness.slide("Vertical navigator height", value = 80)
        reader.verticalNavigatorHeight.get() shouldBe 80
    }

    @Test
    fun invertDoublePagesFollowsLayout() {
        harness.show(SettingsReaderScreen)
        reader.pageLayout.set(0)
        settle()
        val single = enabled("Invert double pages")
        reader.pageLayout.set(1)
        settle()
        enabled("Invert double pages") shouldBe !single
    }
}
