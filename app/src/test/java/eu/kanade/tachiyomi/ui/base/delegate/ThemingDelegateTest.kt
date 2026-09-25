package eu.kanade.tachiyomi.ui.base.delegate

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ThemingDelegateTest {
    @Test
    fun unmappedThemesFallBack() {
        ThemingDelegate.getThemeResIds(AppTheme.DEFAULT, isAmoled = false)
            .shouldContainExactly(R.style.Theme_Tachiyomi)
    }

    @Test
    fun amoledAddsTheOverlay() {
        ThemingDelegate.getThemeResIds(AppTheme.TAKO, isAmoled = true)
            .shouldContainExactly(R.style.Theme_Tachiyomi_Tako, R.style.ThemeOverlay_Tachiyomi_Amoled)
    }

    @Test
    fun everyThemeResolves() {
        AppTheme.entries.map { ThemingDelegate.getThemeResIds(it, isAmoled = false).size }.toSet() shouldBe setOf(1)
    }
}
