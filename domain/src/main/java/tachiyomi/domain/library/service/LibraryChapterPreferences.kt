package tachiyomi.domain.library.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.bookmarkedFilterRaw
import tachiyomi.domain.manga.model.displayMode
import tachiyomi.domain.manga.model.downloadedFilterRaw
import tachiyomi.domain.manga.model.sortDescending
import tachiyomi.domain.manga.model.sorting
import tachiyomi.domain.manga.model.unreadFilterRaw

/**
 * The chapter-list and swipe-action defaults of [LibraryPreferences]: the
 * filter/sort/display flags a new manga starts with and the two swipe actions.
 * Split out of [LibraryPreferences] for the 250-line cap; every name is reached
 * through the subclass exactly as before.
 */
public open class LibraryChapterPreferences(
    private val preferenceStore: PreferenceStore,
) {

    /** Default read/unread chapter filter ([Manga.SHOW_ALL] shows everything). */
    public val filterChapterByRead: Preference<Long> = preferenceStore.getLong(
        "default_chapter_filter_by_read",
        Manga.SHOW_ALL,
    )

    /** Default downloaded chapter filter. */
    public val filterChapterByDownloaded: Preference<Long> = preferenceStore.getLong(
        "default_chapter_filter_by_downloaded",
        Manga.SHOW_ALL,
    )

    /** Default bookmarked chapter filter. */
    public val filterChapterByBookmarked: Preference<Long> = preferenceStore.getLong(
        "default_chapter_filter_by_bookmarked",
        Manga.SHOW_ALL,
    )

    /** Default chapter sort key: source order, chapter number, upload date or name. */
    public val sortChapterBySourceOrNumber: Preference<Long> = preferenceStore.getLong(
        "default_chapter_sort_by_source_or_number",
        Manga.CHAPTER_SORTING_SOURCE,
    )

    /** Default chapter label: the chapter's name or its number. */
    public val displayChapterByNameOrNumber: Preference<Long> = preferenceStore.getLong(
        "default_chapter_display_by_name_or_number",
        Manga.CHAPTER_DISPLAY_NAME,
    )

    /** Default chapter sort direction. */
    public val sortChapterByAscendingOrDescending: Preference<Long> = preferenceStore.getLong(
        "default_chapter_sort_by_ascending_or_descending",
        Manga.CHAPTER_SORT_DESC,
    )

    /** Whether the chapter cache is cleared automatically. */
    public val autoClearChapterCache: Preference<Boolean> =
        preferenceStore.getBoolean("auto_clear_chapter_cache", false)

    /** Action of a swipe from the start edge of a chapter row. */
    public val swipeToStartAction: Preference<LibraryPreferences.ChapterSwipeAction> = preferenceStore.getEnum(
        "pref_chapter_swipe_end_action",
        LibraryPreferences.ChapterSwipeAction.ToggleBookmark,
    )

    /** Action of a swipe from the end edge of a chapter row. */
    public val swipeToEndAction: Preference<LibraryPreferences.ChapterSwipeAction> = preferenceStore.getEnum(
        "pref_chapter_swipe_start_action",
        LibraryPreferences.ChapterSwipeAction.ToggleRead,
    )

    /** Whether library updates may overwrite manga titles. */
    public val updateMangaTitles: Preference<Boolean> =
        preferenceStore.getBoolean("pref_update_library_manga_titles", false)

    /** Whether download file names are restricted to ASCII. */
    public val disallowNonAsciiFilenames: Preference<Boolean> = preferenceStore.getBoolean(
        "disallow_non_ascii_filenames",
        false,
    )

    /** Copies [manga]'s current chapter flags into the defaults above. */
    public fun setChapterSettingsDefault(manga: Manga) {
        filterChapterByRead.set(manga.unreadFilterRaw)
        filterChapterByDownloaded.set(manga.downloadedFilterRaw)
        filterChapterByBookmarked.set(manga.bookmarkedFilterRaw)
        sortChapterBySourceOrNumber.set(manga.sorting)
        displayChapterByNameOrNumber.set(manga.displayMode)
        sortChapterByAscendingOrDescending.set(
            if (manga.sortDescending()) Manga.CHAPTER_SORT_DESC else Manga.CHAPTER_SORT_ASC,
        )
    }
}
