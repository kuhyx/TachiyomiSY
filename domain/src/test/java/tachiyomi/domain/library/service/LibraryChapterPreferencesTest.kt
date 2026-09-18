package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga

internal class LibraryChapterPreferencesTest {

    private val preferences = LibraryChapterPreferences(InMemoryPreferenceStore())

    @Test
    fun filtersDefaultToShowAll() {
        preferences.filterChapterByRead.get() shouldBe Manga.SHOW_ALL
        preferences.filterChapterByDownloaded.get() shouldBe Manga.SHOW_ALL
        preferences.filterChapterByBookmarked.get() shouldBe Manga.SHOW_ALL
        preferences.filterChapterByRead.key() shouldBe "default_chapter_filter_by_read"
        preferences.filterChapterByDownloaded.key() shouldBe "default_chapter_filter_by_downloaded"
        preferences.filterChapterByBookmarked.key() shouldBe "default_chapter_filter_by_bookmarked"
    }

    @Test
    fun sortAndDisplayDefaults() {
        preferences.sortChapterBySourceOrNumber.get() shouldBe Manga.CHAPTER_SORTING_SOURCE
        preferences.displayChapterByNameOrNumber.get() shouldBe Manga.CHAPTER_DISPLAY_NAME
        preferences.sortChapterByAscendingOrDescending.get() shouldBe Manga.CHAPTER_SORT_DESC
        preferences.sortChapterBySourceOrNumber.key() shouldBe "default_chapter_sort_by_source_or_number"
        preferences.displayChapterByNameOrNumber.key() shouldBe "default_chapter_display_by_name_or_number"
        preferences.sortChapterByAscendingOrDescending.key() shouldBe "default_chapter_sort_by_ascending_or_descending"
    }

    @Test
    fun booleanDefaults() {
        preferences.autoClearChapterCache.get() shouldBe false
        preferences.updateMangaTitles.get() shouldBe false
        preferences.disallowNonAsciiFilenames.get() shouldBe false
        preferences.autoClearChapterCache.key() shouldBe "auto_clear_chapter_cache"
        preferences.updateMangaTitles.key() shouldBe "pref_update_library_manga_titles"
        preferences.disallowNonAsciiFilenames.key() shouldBe "disallow_non_ascii_filenames"
    }

    @Test
    fun swipeActionDefaults() {
        // The keys are historically crossed: the start swipe is stored under the "end" key.
        preferences.swipeToStartAction.get() shouldBe LibraryPreferences.ChapterSwipeAction.ToggleBookmark
        preferences.swipeToStartAction.key() shouldBe "pref_chapter_swipe_end_action"
        preferences.swipeToEndAction.get() shouldBe LibraryPreferences.ChapterSwipeAction.ToggleRead
        preferences.swipeToEndAction.key() shouldBe "pref_chapter_swipe_start_action"
    }

    @Test
    fun copiesDescendingMangaFlags() {
        val filters = Manga.CHAPTER_SHOW_UNREAD or Manga.CHAPTER_SHOW_DOWNLOADED or Manga.CHAPTER_SHOW_BOOKMARKED
        val layout = Manga.CHAPTER_SORTING_NUMBER or Manga.CHAPTER_DISPLAY_NUMBER or Manga.CHAPTER_SORT_DESC
        val manga = Manga.create().copy(chapterFlags = filters or layout)

        preferences.setChapterSettingsDefault(manga)

        preferences.filterChapterByRead.get() shouldBe Manga.CHAPTER_SHOW_UNREAD
        preferences.filterChapterByDownloaded.get() shouldBe Manga.CHAPTER_SHOW_DOWNLOADED
        preferences.filterChapterByBookmarked.get() shouldBe Manga.CHAPTER_SHOW_BOOKMARKED
        preferences.sortChapterBySourceOrNumber.get() shouldBe Manga.CHAPTER_SORTING_NUMBER
        preferences.displayChapterByNameOrNumber.get() shouldBe Manga.CHAPTER_DISPLAY_NUMBER
        preferences.sortChapterByAscendingOrDescending.get() shouldBe Manga.CHAPTER_SORT_DESC
    }

    @Test
    fun copiesAscendingMangaFlags() {
        val manga = Manga.create().copy(
            chapterFlags = Manga.CHAPTER_SHOW_READ or Manga.CHAPTER_SORTING_UPLOAD_DATE or Manga.CHAPTER_SORT_ASC,
        )

        preferences.setChapterSettingsDefault(manga)

        preferences.filterChapterByRead.get() shouldBe Manga.CHAPTER_SHOW_READ
        preferences.filterChapterByDownloaded.get() shouldBe Manga.SHOW_ALL
        preferences.filterChapterByBookmarked.get() shouldBe Manga.SHOW_ALL
        preferences.sortChapterBySourceOrNumber.get() shouldBe Manga.CHAPTER_SORTING_UPLOAD_DATE
        preferences.displayChapterByNameOrNumber.get() shouldBe Manga.CHAPTER_DISPLAY_NAME
        preferences.sortChapterByAscendingOrDescending.get() shouldBe Manga.CHAPTER_SORT_ASC
    }
}
