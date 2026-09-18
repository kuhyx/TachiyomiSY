package tachiyomi.domain.library.service

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort

internal class LibraryPreferencesTest {

    private val preferences = LibraryPreferences(InMemoryPreferenceStore())

    @Test
    fun displayDefaults() {
        preferences.displayMode.get() shouldBe LibraryDisplayMode.default
        preferences.displayMode.key() shouldBe "pref_display_mode_library"
        preferences.sortingMode.get() shouldBe LibrarySort.default
        preferences.sortingMode.key() shouldBe "library_sorting_mode"
        preferences.randomSortSeed.get() shouldBe 0
        preferences.portraitColumns.get() shouldBe 0
        preferences.landscapeColumns.get() shouldBe 0
        preferences.showContinueReadingButton.get() shouldBe false
    }

    @Test
    fun updateDefaults() {
        preferences.lastUpdatedTimestamp.get() shouldBe 0L
        Preference.isAppState(preferences.lastUpdatedTimestamp.key()) shouldBe true
        preferences.autoUpdateInterval.get() shouldBe 0
        preferences.autoUpdateDeviceRestrictions.get() shouldBe setOf(LibraryPreferences.DEVICE_ONLY_ON_WIFI)
        preferences.autoUpdateMangaRestrictions.get() shouldBe setOf(
            LibraryPreferences.MANGA_HAS_UNREAD,
            LibraryPreferences.MANGA_NON_COMPLETED,
            LibraryPreferences.MANGA_NON_READ,
            LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD,
        )
        preferences.autoUpdateMetadata.get() shouldBe false
        preferences.markDuplicateReadChapterAsRead.get() shouldBe emptySet()
    }

    @Test
    fun filterDefaults() {
        preferences.filterDownloaded.get() shouldBe TriState.DISABLED
        preferences.filterUnread.get() shouldBe TriState.DISABLED
        preferences.filterStarted.get() shouldBe TriState.DISABLED
        preferences.filterBookmarked.get() shouldBe TriState.DISABLED
        preferences.filterCompleted.get() shouldBe TriState.DISABLED
        preferences.filterIntervalCustom.get() shouldBe TriState.DISABLED
        preferences.filterLewd.get() shouldBe TriState.DISABLED
        preferences.filterLewd.key() shouldBe "pref_filter_library_lewd_v2"
    }

    @Test
    fun filterTrackingIsPerTracker() {
        val tracked = preferences.filterTracking(3)
        tracked.get() shouldBe TriState.DISABLED
        tracked.key() shouldBe "pref_filter_library_tracked_3_v2"
        preferences.filterTracking(7).key() shouldBe "pref_filter_library_tracked_7_v2"
    }

    @Test
    fun groupingDefaults() {
        preferences.sortTagsForLibrary.get() shouldBe emptySet()
        preferences.groupLibraryUpdateType.get() shouldBe GroupLibraryMode.GLOBAL
        preferences.groupLibraryBy.get() shouldBe LibraryGroup.BY_DEFAULT
        preferences.groupLibraryBy.key() shouldBe "group_library_by"
    }

    @Test
    fun storesNewValues() {
        preferences.displayMode.set(LibraryDisplayMode.List)
        preferences.sortingMode.set(LibrarySort(LibrarySort.Type.Random, LibrarySort.Direction.Descending))
        preferences.displayMode.get() shouldBe LibraryDisplayMode.List
        preferences.sortingMode.get().type shouldBe LibrarySort.Type.Random
    }

    @Test
    fun swipeActionsInMenuOrder() {
        LibraryPreferences.ChapterSwipeAction.entries shouldContainExactly listOf(
            LibraryPreferences.ChapterSwipeAction.ToggleRead,
            LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
            LibraryPreferences.ChapterSwipeAction.Download,
            LibraryPreferences.ChapterSwipeAction.Disabled,
        )
        val download = LibraryPreferences.ChapterSwipeAction.Download
        LibraryPreferences.ChapterSwipeAction.valueOf("Download") shouldBe download
    }

    @Test
    fun constantsAreStable() {
        LibraryPreferences.DEVICE_ONLY_ON_WIFI shouldBe "wifi"
        LibraryPreferences.DEVICE_NETWORK_NOT_METERED shouldBe "network_not_metered"
        LibraryPreferences.DEVICE_CHARGING shouldBe "ac"
        LibraryPreferences.MANGA_NON_COMPLETED shouldBe "manga_ongoing"
        LibraryPreferences.MANGA_HAS_UNREAD shouldBe "manga_fully_read"
        LibraryPreferences.MANGA_NON_READ shouldBe "manga_started"
        LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD shouldBe "manga_outside_release_period"
        LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_NEW shouldBe "new"
        LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING shouldBe "existing"
        LibraryPreferences.DEFAULT_CATEGORY_PREF_KEY shouldBe "default_category"
    }

    @Test
    fun categoryKeysNameEveryPref() {
        LibraryPreferences.categoryPreferenceKeys shouldBe setOf(
            preferences.defaultCategory.key(),
            preferences.updateCategories.key(),
            preferences.updateCategoriesExclude.key(),
        )
    }
}
