package eu.kanade.domain.ui.model

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.i18n.MR

internal class AppThemeTest {

    @Test
    fun namedThemesHaveTitles() {
        AppTheme.entries.size shouldBe 18
        AppTheme.DEFAULT.titleRes shouldBe MR.strings.label_default
        AppTheme.MONET.titleRes shouldBe MR.strings.theme_monet
        AppTheme.MONOCHROME.titleRes shouldBe MR.strings.theme_monochrome
        AppTheme.valueOf("TAKO") shouldBe AppTheme.TAKO
    }

    @Test
    fun deprecatedThemesHaveNone() {
        listOf(AppTheme.DARK_BLUE, AppTheme.HOT_PINK, AppTheme.BLUE, AppTheme.PURE_RED).forEach {
            it.titleRes.shouldBeNull()
        }
    }
}
