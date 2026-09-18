package exh.source

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

internal class ExhPreferencesTest {

    private val preferences = ExhPreferences(InMemoryPreferenceStore())

    @Test
    fun featureDefaults() {
        preferences.isHentaiEnabled.get() shouldBe true
        preferences.enableExhentai.get() shouldBe false
        preferences.imageQuality.get() shouldBe "auto"
        preferences.useHentaiAtHome.get() shouldBe 0
        preferences.useJapaneseTitle.get() shouldBe false
        preferences.exhUseOriginalImages.get() shouldBe false
        preferences.ehTagFilterValue.get() shouldBe 0
        preferences.ehTagWatchingValue.get() shouldBe 0
        preferences.enhancedEHentaiView.get() shouldBe true
    }

    @Test
    fun cookieDefaultsArePrivate() {
        preferences.memberIdVal.get() shouldBe ""
        preferences.passHashVal.get() shouldBe ""
        preferences.igneousVal.get() shouldBe ""
        preferences.exhSettingsKey.get() shouldBe ""
        preferences.exhSessionCookie.get() shouldBe ""
        preferences.exhHathPerksCookies.get() shouldBe ""
        Preference.isPrivate(preferences.memberIdVal.key()) shouldBe true
        Preference.isPrivate(preferences.enableExhentai.key()) shouldBe true
        Preference.isPrivate(preferences.isHentaiEnabled.key()) shouldBe false
    }

    @Test
    fun syncDefaults() {
        preferences.exhShowSyncIntro.get() shouldBe true
        preferences.exhReadOnlySync.get() shouldBe false
        preferences.exhLenientSync.get() shouldBe false
        preferences.exhShowSettingsUploadWarning.get() shouldBe true
        preferences.logLevel.get() shouldBe 0
        preferences.exhAutoUpdateFrequency.get() shouldBe 1
        preferences.exhAutoUpdateRequirements.get() shouldBe emptySet()
        preferences.exhAutoUpdateStats.get() shouldBe ""
        Preference.isAppState(preferences.exhAutoUpdateStats.key()) shouldBe true
        preferences.exhWatchedListDefaultState.get() shouldBe false
    }

    @Test
    fun filterGridDefaults() {
        val languages = preferences.exhSettingsLanguages.get().lines()

        languages.size shouldBe 17
        languages.all { it == "false*false*false" } shouldBe true
        preferences.exhEnabledCategories.get() shouldBe List(10) { "false" }.joinToString(",")
    }

    @Test
    fun settingsProfilePicksSite() {
        preferences.ehSettingsProfile.get() shouldBe -1
        preferences.exhSettingsProfile.get() shouldBe -1

        preferences.settingsProfile(exh = true) shouldBe preferences.exhSettingsProfile
        preferences.settingsProfile(exh = false) shouldBe preferences.ehSettingsProfile
    }
}
