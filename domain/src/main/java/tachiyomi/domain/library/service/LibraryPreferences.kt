package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort

/**
 * Every user setting of the library screen. The display, update and filter
 * settings live here; badges and categories in [LibraryCategoryPreferences];
 * chapter defaults and swipe actions in [LibraryChapterPreferences].
 */
public class LibraryPreferences(
    private val preferenceStore: PreferenceStore,
) : LibraryCategoryPreferences(preferenceStore) {

    /** Grid or list layout of the library. */
    public val displayMode: Preference<LibraryDisplayMode> = preferenceStore.getObjectFromString(
        "pref_display_mode_library",
        LibraryDisplayMode.default,
        LibraryDisplayMode.Serializer::serialize,
        LibraryDisplayMode.Serializer::deserialize,
    )

    /** Sort key and direction of the library. */
    public val sortingMode: Preference<LibrarySort> = preferenceStore.getObjectFromString(
        "library_sorting_mode",
        LibrarySort.default,
        LibrarySort.Serializer::serialize,
        LibrarySort.Serializer::deserialize,
    )

    /** Seed of the random sort, so the order is stable until reshuffled. */
    public val randomSortSeed: Preference<Int> = preferenceStore.getInt("library_random_sort_seed", 0)

    /** Grid columns in portrait; 0 picks automatically. */
    public val portraitColumns: Preference<Int> = preferenceStore.getInt("pref_library_columns_portrait_key", 0)

    /** Grid columns in landscape; 0 picks automatically. */
    public val landscapeColumns: Preference<Int> = preferenceStore.getInt("pref_library_columns_landscape_key", 0)

    /** Epoch millis of the last library update; app state. */
    public val lastUpdatedTimestamp: Preference<Long> = preferenceStore.getLong(
        Preference.appStateKey("library_update_last_timestamp"),
        0L,
    )

    /** Hours between automatic library updates; 0 disables them. */
    public val autoUpdateInterval: Preference<Int> = preferenceStore.getInt("pref_library_update_interval_key", 0)

    /** Device conditions ([DEVICE_ONLY_ON_WIFI], ...) an automatic update waits for. */
    public val autoUpdateDeviceRestrictions: Preference<Set<String>> = preferenceStore.getStringSet(
        "library_update_restriction",
        setOf(
            DEVICE_ONLY_ON_WIFI,
        ),
    )

    /** Manga conditions ([MANGA_HAS_UNREAD], ...) that exclude an entry from automatic updates. */
    public val autoUpdateMangaRestrictions: Preference<Set<String>> = preferenceStore.getStringSet(
        "library_update_manga_restriction",
        setOf(
            MANGA_HAS_UNREAD,
            MANGA_NON_COMPLETED,
            MANGA_NON_READ,
            MANGA_OUTSIDE_RELEASE_PERIOD,
        ),
    )

    /** Whether library updates also refresh manga details. */
    public val autoUpdateMetadata: Preference<Boolean> = preferenceStore.getBoolean("auto_update_metadata", false)

    /** Whether covers show a "continue reading" button. */
    public val showContinueReadingButton: Preference<Boolean> = preferenceStore.getBoolean(
        "display_continue_reading_button",
        false,
    )

    /** Which duplicate chapters ([MARK_DUPLICATE_CHAPTER_READ_NEW], ...) are marked read together. */
    public val markDuplicateReadChapterAsRead: Preference<Set<String>> = preferenceStore.getStringSet(
        "mark_duplicate_read_chapter_read",
        emptySet(),
    )

    // region Filter

    /** Library filter on downloaded chapters. */
    public val filterDownloaded: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_downloaded_v2",
        TriState.DISABLED,
    )

    /** Library filter on unread chapters. */
    public val filterUnread: Preference<TriState> =
        preferenceStore.getEnum("pref_filter_library_unread_v2", TriState.DISABLED)

    /** Library filter on started entries. */
    public val filterStarted: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_started_v2",
        TriState.DISABLED,
    )

    /** Library filter on bookmarked chapters. */
    public val filterBookmarked: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_bookmarked_v2",
        TriState.DISABLED,
    )

    /** Library filter on completed entries. */
    public val filterCompleted: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_completed_v2",
        TriState.DISABLED,
    )

    /** Library filter on entries with a custom fetch interval. */
    public val filterIntervalCustom: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_interval_custom",
        TriState.DISABLED,
    )

    // SY -->

    /** Library filter on lewd entries. */
    public val filterLewd: Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_lewd_v2",
        TriState.DISABLED,
    )
    // SY <--

    // endregion

    // SY -->

    /** Tags the library is sorted by when [LibrarySort.Type.TagList] is active. */
    public val sortTagsForLibrary: Preference<Set<String>> =
        preferenceStore.getStringSet("sort_tags_for_library", mutableSetOf())

    /** Whether library updates run globally or per group. */
    public val groupLibraryUpdateType: Preference<GroupLibraryMode> = preferenceStore.getEnum(
        "group_library_update_type",
        GroupLibraryMode.GLOBAL,
    )

    /** Grouping of the library ([LibraryGroup] constant). */
    public val groupLibraryBy: Preference<Int> = preferenceStore.getInt("group_library_by", LibraryGroup.BY_DEFAULT)

    // SY <--

    /** Library filter on tracker [id]. */
    public fun filterTracking(id: Int): Preference<TriState> = preferenceStore.getEnum(
        "pref_filter_library_tracked_${id}_v2",
        TriState.DISABLED,
    )

    /** What a swipe on a chapter row does. */
    public enum class ChapterSwipeAction {
        /** Marks the chapter read or unread. */
        ToggleRead,

        /** Bookmarks or unbookmarks the chapter. */
        ToggleBookmark,

        /** Downloads or deletes the chapter. */
        Download,

        /** Does nothing. */
        Disabled,
    }

    /** The string values the restriction and swipe preferences are stored as. */
    public companion object {
        /** [autoUpdateDeviceRestrictions]: only on Wi-Fi. */
        public const val DEVICE_ONLY_ON_WIFI: String = "wifi"

        /** [autoUpdateDeviceRestrictions]: only on unmetered networks. */
        public const val DEVICE_NETWORK_NOT_METERED: String = "network_not_metered"

        /** [autoUpdateDeviceRestrictions]: only while charging. */
        public const val DEVICE_CHARGING: String = "ac"

        /** [autoUpdateMangaRestrictions]: skip completed entries. */
        public const val MANGA_NON_COMPLETED: String = "manga_ongoing"

        /** [autoUpdateMangaRestrictions]: skip entries with unread chapters. */
        public const val MANGA_HAS_UNREAD: String = "manga_fully_read"

        /** [autoUpdateMangaRestrictions]: skip entries not started. */
        public const val MANGA_NON_READ: String = "manga_started"

        /** [autoUpdateMangaRestrictions]: skip entries outside their expected release window. */
        public const val MANGA_OUTSIDE_RELEASE_PERIOD: String = "manga_outside_release_period"

        /** [markDuplicateReadChapterAsRead]: mark new duplicates. */
        public const val MARK_DUPLICATE_CHAPTER_READ_NEW: String = "new"

        /** [markDuplicateReadChapterAsRead]: mark existing duplicates. */
        public const val MARK_DUPLICATE_CHAPTER_READ_EXISTING: String = "existing"

        /** Preference key of [defaultCategory]. */
        public const val DEFAULT_CATEGORY_PREF_KEY: String = "default_category"
        internal const val LIBRARY_UPDATE_CATEGORIES_PREF_KEY = "library_update_categories"
        internal const val LIBRARY_UPDATE_CATEGORIES_EXCLUDE_PREF_KEY = "library_update_categories_exclude"

        /** Keys whose values name categories, reset when categories are deleted. */
        public val categoryPreferenceKeys: Set<String> = setOf(
            DEFAULT_CATEGORY_PREF_KEY,
            LIBRARY_UPDATE_CATEGORIES_PREF_KEY,
            LIBRARY_UPDATE_CATEGORIES_EXCLUDE_PREF_KEY,
        )
    }
}
