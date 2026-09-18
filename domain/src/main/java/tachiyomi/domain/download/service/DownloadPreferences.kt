package tachiyomi.domain.download.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

/**
 * Every user setting of chapter downloading: how chapters are fetched and stored, when
 * downloaded chapters are deleted, and which new chapters are downloaded automatically.
 */
public class DownloadPreferences(
    preferenceStore: PreferenceStore,
) {

    /** Whether downloads wait for a Wi-Fi connection; on by default. */
    public val downloadOnlyOverWifi: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_download_only_over_wifi_key",
        true,
    )

    /** Whether a finished chapter is packed into one CBZ archive instead of a folder; on by default. */
    public val saveChaptersAsCBZ: Preference<Boolean> = preferenceStore.getBoolean("save_chapter_as_cbz", true)

    /** Whether very tall pages are cut into several images while downloading; on by default. */
    public val splitTallImages: Preference<Boolean> = preferenceStore.getBoolean("split_tall_images", true)

    /** How many unread chapters ahead the reader downloads while reading; 0 (the default) disables it. */
    public val autoDownloadWhileReading: Preference<Int> = preferenceStore.getInt("auto_download_while_reading", 0)

    /**
     * Which earlier chapter the reader deletes once a chapter is read: 0 the chapter just read, 1 the
     * one before it and so on; -1 (the default) disables it.
     */
    public val removeAfterReadSlots: Preference<Int> = preferenceStore.getInt("remove_after_read_slots", -1)

    /** Whether marking a chapter as read deletes its download; off by default. */
    public val removeAfterMarkedAsRead: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_remove_after_marked_as_read_key",
        false,
    )

    /** Whether automatic deletion may remove bookmarked chapters; off by default. */
    public val removeBookmarkedChapters: Preference<Boolean> =
        preferenceStore.getBoolean("pref_remove_bookmarked", false)

    /** Ids of the categories whose manga keep their read chapters when others are deleted; none by default. */
    public val removeExcludeCategories: Preference<Set<String>> = preferenceStore.getStringSet(
        REMOVE_EXCLUDE_CATEGORIES_PREF_KEY,
        emptySet(),
    )

    /** Whether new chapters found by a library update are downloaded; off by default. */
    public val downloadNewChapters: Preference<Boolean> = preferenceStore.getBoolean("download_new", false)

    /** Ids of the categories whose manga get new chapters downloaded; empty (the default) means all. */
    public val downloadNewChapterCategories: Preference<Set<String>> = preferenceStore.getStringSet(
        DOWNLOAD_NEW_CATEGORIES_PREF_KEY,
        emptySet(),
    )

    /** Ids of the categories whose manga never get new chapters downloaded; none by default. */
    public val downloadNewChapterCategoriesExclude: Preference<Set<String>> = preferenceStore.getStringSet(
        DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY,
        emptySet(),
    )

    /** Whether a new chapter whose number was already read is skipped; off by default. */
    public val downloadNewUnreadChaptersOnly: Preference<Boolean> = preferenceStore.getBoolean(
        "download_new_unread_chapters_only",
        false,
    )

    /** How many sources are downloaded from at the same time; 5 by default. */
    public val parallelSourceLimit: Preference<Int> =
        preferenceStore.getInt("download_parallel_source_limit", DEFAULT_PARALLEL)

    /** How many pages of one chapter are fetched at the same time; 5 by default. */
    public val parallelPageLimit: Preference<Int> =
        preferenceStore.getInt("download_parallel_page_limit", DEFAULT_PARALLEL)

    // SY -->

    /** Whether the first six characters of the chapter url's MD5 end the chapter's file name; on by default. */
    public val includeChapterUrlHash: Preference<Boolean> =
        preferenceStore.getBoolean("download_include_chapter_url_hash", true)
    // SY <--

    /** The preference keys that name categories. */
    public companion object {
        private const val DEFAULT_PARALLEL = 5
        private const val REMOVE_EXCLUDE_CATEGORIES_PREF_KEY = "remove_exclude_categories"
        private const val DOWNLOAD_NEW_CATEGORIES_PREF_KEY = "download_new_categories"
        private const val DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY = "download_new_categories_exclude"

        /** Keys whose values name categories, reset when categories are deleted. */
        public val categoryPreferenceKeys: Set<String> = setOf(
            REMOVE_EXCLUDE_CATEGORIES_PREF_KEY,
            DOWNLOAD_NEW_CATEGORIES_PREF_KEY,
            DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY,
        )
    }
}
