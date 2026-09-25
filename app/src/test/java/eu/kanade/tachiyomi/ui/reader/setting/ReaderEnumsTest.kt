package eu.kanade.tachiyomi.ui.reader.setting

import android.content.pm.ActivityInfo
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class ReaderEnumsTest {

    @Test
    fun bottomButtonMembership() {
        ReaderBottomButton.ViewChapters.isIn(setOf("vc")) shouldBe true
        ReaderBottomButton.Share.isIn(ReaderBottomButton.BUTTONS_DEFAULTS) shouldBe false
        ReaderBottomButton.BUTTONS_DEFAULTS shouldBe setOf("vc", "wb", "cbp", "cbc", "pl")
        ReaderBottomButton.entries.map { it.value }.toSet().size shouldBe ReaderBottomButton.entries.size
    }

    @Test
    fun orientationFromPreference() {
        ReaderOrientation.entries.forEach { ReaderOrientation.fromPreference(it.flagValue) shouldBe it }
        ReaderOrientation.fromPreference(null) shouldBe ReaderOrientation.DEFAULT
        ReaderOrientation.fromPreference(0x7) shouldBe ReaderOrientation.DEFAULT
        ReaderOrientation.LOCKED_LANDSCAPE.flag shouldBe ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ReaderOrientation.MASK shouldBe 0x38
    }

    @Test
    fun readingModeFromPreference() {
        ReadingMode.entries.forEach { ReadingMode.fromPreference(it.flagValue) shouldBe it }
        ReadingMode.fromPreference(null) shouldBe ReadingMode.DEFAULT
        ReadingMode.fromPreference(0x6) shouldBe ReadingMode.DEFAULT
        ReadingMode.MASK shouldBe 0x7
    }

    @Test
    fun readingModeTypes() {
        ReadingMode.isPagerType(ReadingMode.VERTICAL.flagValue) shouldBe true
        ReadingMode.isPagerType(ReadingMode.WEBTOON.flagValue) shouldBe false
        ReadingMode.isPagerType(ReadingMode.DEFAULT.flagValue) shouldBe false
        ReadingMode.DEFAULT.direction shouldBe null
        ReadingMode.WEBTOON.direction shouldBe ReadingMode.Direction.Vertical
        ReadingMode.LEFT_TO_RIGHT.direction shouldBe ReadingMode.Direction.Horizontal
        ReadingMode.CONTINUOUS_VERTICAL.type shouldBe ReadingMode.ViewerType.Webtoon
    }

    @Test
    fun defaultModeHasNoViewer() {
        val error = shouldThrow<IllegalStateException> {
            ReadingMode.toViewer(null, mockk<ReaderActivity>())
        }
        error.message shouldBe "Preference value must be resolved: null"
    }

    @Test
    fun colorFiltersBeforePie() {
        // Plain JVM: the android.jar stub reports SDK 0, so only the three pre-P blend modes exist.
        ReaderPreferences.ColorFilterMode.size shouldBe 3
    }
}
