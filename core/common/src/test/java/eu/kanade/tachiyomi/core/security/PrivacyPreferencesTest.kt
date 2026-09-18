package eu.kanade.tachiyomi.core.security

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class PrivacyPreferencesTest {
    private val prefs = PrivacyPreferences(InMemoryPreferenceStore())

    @Test
    fun reportingIsOnByDefault() {
        prefs.crashlytics.get() shouldBe true
        prefs.analytics.get() shouldBe true
    }

    @Test
    fun usesStableKeys() {
        prefs.crashlytics.key() shouldBe "crashlytics"
        prefs.analytics.key() shouldBe "analytics"
    }

    @Test
    fun canBeOptedOut() {
        prefs.crashlytics.set(false)
        prefs.analytics.set(false)
        prefs.crashlytics.get() shouldBe false
        prefs.analytics.get() shouldBe false
    }
}
