package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

internal class LibraryCategoryPreferencesTest {

    private val preferences = LibraryCategoryPreferences(InMemoryPreferenceStore())

    @Test
    fun badgeDefaults() {
        preferences.downloadBadge.get() shouldBe false
        preferences.unreadBadge.get() shouldBe true
        preferences.localBadge.get() shouldBe true
        preferences.languageBadge.get() shouldBe false
        preferences.newShowUpdatesCount.get() shouldBe true
        preferences.newUpdatesCount.get() shouldBe 0
    }

    @Test
    fun badgeKeys() {
        preferences.downloadBadge.key() shouldBe "display_download_badge"
        preferences.unreadBadge.key() shouldBe "display_unread_badge"
        preferences.localBadge.key() shouldBe "display_local_badge"
        preferences.languageBadge.key() shouldBe "display_language_badge"
        preferences.newShowUpdatesCount.key() shouldBe "library_show_updates_count"
        Preference.isAppState(preferences.newUpdatesCount.key()) shouldBe true
    }

    @Test
    fun categoryDefaults() {
        preferences.defaultCategory.get() shouldBe -1
        preferences.lastUsedCategory.get() shouldBe 0
        preferences.categoryTabs.get() shouldBe true
        preferences.categoryNumberOfItems.get() shouldBe false
        preferences.categorizedDisplaySettings.get() shouldBe false
        preferences.updateCategories.get() shouldBe emptySet()
        preferences.updateCategoriesExclude.get() shouldBe emptySet()
    }

    @Test
    fun categoryKeys() {
        preferences.defaultCategory.key() shouldBe LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY
        Preference.isAppState(preferences.lastUsedCategory.key()) shouldBe true
        preferences.categoryTabs.key() shouldBe "display_category_tabs"
        preferences.categoryNumberOfItems.key() shouldBe "display_number_of_items"
        preferences.categorizedDisplaySettings.key() shouldBe "categorized_display"
        preferences.updateCategories.key() shouldBe "library_update_categories"
        preferences.updateCategoriesExclude.key() shouldBe "library_update_categories_exclude"
    }

    @Test
    fun inheritsChapterDefaults() {
        preferences.autoClearChapterCache.get() shouldBe false
        preferences.updateCategories.set(setOf("1"))
        preferences.updateCategories.get() shouldBe setOf("1")
    }
}
