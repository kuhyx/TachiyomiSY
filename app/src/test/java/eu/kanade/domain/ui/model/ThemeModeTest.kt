package eu.kanade.domain.ui.model

import androidx.appcompat.app.AppCompatDelegate
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class ThemeModeTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun hasThreeModes() {
        ThemeMode.entries shouldBe listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)
        ThemeMode.valueOf("DARK") shouldBe ThemeMode.DARK
    }

    @Test
    fun mapsEachModeOntoTheDelegate() {
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } returns Unit
        setAppCompatDelegateThemeMode(ThemeMode.LIGHT)
        setAppCompatDelegateThemeMode(ThemeMode.DARK)
        setAppCompatDelegateThemeMode(ThemeMode.SYSTEM)
        verify(exactly = 1) { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        verify(exactly = 1) { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        verify(exactly = 1) { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
    }
}
