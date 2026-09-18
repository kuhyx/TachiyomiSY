package eu.kanade.tachiyomi.network

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class NetworkPreferencesTest {
    @Test
    fun defaultsWhenStoreIsEmpty() {
        val preferences = NetworkPreferences(InMemoryPreferenceStore())
        preferences.verboseLogging.get() shouldBe false
        preferences.dohProvider.get() shouldBe -1
        preferences.defaultUserAgent.get() shouldContain "Chrome/141.0.0.0 Mobile Safari/537.36"
    }

    @Test
    fun verboseDefaultIsConfigurable() {
        NetworkPreferences(InMemoryPreferenceStore(), verboseLoggingDefault = true).verboseLogging.get() shouldBe true
    }

    @Test
    fun readsStoredValues() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("verbose_logging", true, false),
                InMemoryPreferenceStore.InMemoryPreference("doh_provider", PREF_DOH_QUAD9, -1),
                InMemoryPreferenceStore.InMemoryPreference("default_user_agent", "agent/1", ""),
            ),
        )
        val preferences = NetworkPreferences(store)
        preferences.verboseLogging.get() shouldBe true
        preferences.dohProvider.get() shouldBe PREF_DOH_QUAD9
        preferences.defaultUserAgent.get() shouldBe "agent/1"
    }
}
