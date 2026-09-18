package tachiyomi.domain.download.service

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

internal class DownloadPreferencesTest {

    private val preferences = DownloadPreferences(InMemoryPreferenceStore())

    @Test
    fun fetchDefaults() {
        preferences.downloadOnlyOverWifi.get() shouldBe true
        preferences.saveChaptersAsCBZ.get() shouldBe true
        preferences.splitTallImages.get() shouldBe true
        preferences.autoDownloadWhileReading.get() shouldBe 0
        preferences.parallelSourceLimit.get() shouldBe 5
        preferences.parallelPageLimit.get() shouldBe 5
        preferences.includeChapterUrlHash.get() shouldBe true
    }

    @Test
    fun deletionDefaults() {
        preferences.removeAfterReadSlots.get() shouldBe -1
        preferences.removeAfterMarkedAsRead.get() shouldBe false
        preferences.removeBookmarkedChapters.get() shouldBe false
        preferences.removeExcludeCategories.get() shouldBe emptySet()
    }

    @Test
    fun newChapterDefaults() {
        preferences.downloadNewChapters.get() shouldBe false
        preferences.downloadNewChapterCategories.get() shouldBe emptySet()
        preferences.downloadNewChapterCategoriesExclude.get() shouldBe emptySet()
        preferences.downloadNewUnreadChaptersOnly.get() shouldBe false
    }

    @Test
    fun categoryKeysMatchPreferences() {
        DownloadPreferences.categoryPreferenceKeys shouldContainExactly setOf(
            preferences.removeExcludeCategories.key(),
            preferences.downloadNewChapterCategories.key(),
            preferences.downloadNewChapterCategoriesExclude.key(),
        )
    }

    @Test
    fun preferencesHoldValues() {
        preferences.downloadNewChapterCategories.set(setOf("1", "2"))
        preferences.parallelSourceLimit.set(2)

        preferences.downloadNewChapterCategories.get() shouldBe setOf("1", "2")
        preferences.parallelSourceLimit.get() shouldBe 2
    }
}
