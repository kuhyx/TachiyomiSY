package tachiyomi.domain.updates.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.TriState

internal class UpdatesPreferencesTest {

    private val preferences = UpdatesPreferences(InMemoryPreferenceStore())

    @Test
    fun filtersDefaultToDisabled() {
        preferences.filterDownloaded.get() shouldBe TriState.DISABLED
        preferences.filterUnread.get() shouldBe TriState.DISABLED
        preferences.filterStarted.get() shouldBe TriState.DISABLED
        preferences.filterBookmarked.get() shouldBe TriState.DISABLED
        preferences.filterExcludedScanlators.get() shouldBe false
    }

    @Test
    fun keysAreStable() {
        preferences.filterDownloaded.key() shouldBe "pref_filter_updates_downloaded"
        preferences.filterUnread.key() shouldBe "pref_filter_updates_unread"
        preferences.filterStarted.key() shouldBe "pref_filter_updates_started"
        preferences.filterBookmarked.key() shouldBe "pref_filter_updates_bookmarked"
        preferences.filterExcludedScanlators.key() shouldBe "pref_filter_updates_hide_excluded_scanlators"
    }

    @Test
    fun filtersHoldValues() {
        preferences.filterUnread.set(TriState.ENABLED_NOT)
        preferences.filterExcludedScanlators.set(true)

        preferences.filterUnread.get() shouldBe TriState.ENABLED_NOT
        preferences.filterExcludedScanlators.get() shouldBe true
    }
}
