package eu.kanade.domain.ui

import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isDynamicColorAvailable
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal class UiPreferencesTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun defaultsWithoutDynamicColor() {
        mockkStatic("eu.kanade.tachiyomi.util.system.DeviceUtilExtensionsKt")
        every { DeviceUtil.isDynamicColorAvailable } returns false
        val preferences = UiPreferences(InMemoryPreferenceStore())
        preferences.themeMode.get() shouldBe ThemeMode.SYSTEM
        preferences.appTheme.get() shouldBe AppTheme.DEFAULT
        preferences.themeDarkAmoled.get() shouldBe false
        preferences.relativeTime.get() shouldBe true
        preferences.dateFormat.get() shouldBe ""
        preferences.tabletUiMode.get() shouldBe TabletUiMode.AUTOMATIC
        preferences.imagesInDescription.get() shouldBe true
        preferences.expandFilters.get() shouldBe false
        preferences.hideFeedTab.get() shouldBe false
        preferences.feedTabInFront.get() shouldBe false
        preferences.recommendsInOverflow.get() shouldBe false
        preferences.mergeInOverflow.get() shouldBe true
        preferences.previewsRowCount.get() shouldBe 4
        preferences.useNewSourceNavigation.get() shouldBe true
        preferences.bottomBarLabels.get() shouldBe true
        preferences.showNavUpdates.get() shouldBe true
        preferences.showNavHistory.get() shouldBe true
    }

    @Test
    fun dynamicColorDefaultsToMonet() {
        mockkStatic("eu.kanade.tachiyomi.util.system.DeviceUtilExtensionsKt")
        every { DeviceUtil.isDynamicColorAvailable } returns true
        UiPreferences(InMemoryPreferenceStore()).appTheme.get() shouldBe AppTheme.MONET
    }

    @Test
    fun dateFormatFallsBackToLocale() {
        val date = LocalDate.of(2024, 3, 9)
        UiPreferences.dateFormat("").format(date) shouldBe
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(date)
        UiPreferences.dateFormat("yyyy-MM-dd").format(date) shouldBe "2024-03-09"
    }
}
