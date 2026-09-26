package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.ItemPreferences
import io.mockk.mockk
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track

/** A non-favourite manga (so no custom-info lookup) titled [title] from [source]. */
internal fun libManga(id: Long, title: String = "Manga $id", source: Long = 1L): Manga =
    Manga.create().copy(id = id, ogTitle = title, source = source, url = "/m/$id")

/** [manga] in the library under [categories]; the counters default to an untouched entry. */
internal fun libEntry(
    manga: Manga,
    categories: List<Long> = listOf(1L),
    total: Long = 0L,
    read: Long = 0L,
    bookmarks: Long = 0L,
): LibraryManga = LibraryManga(
    manga = manga,
    categories = categories,
    totalChapters = total,
    readCount = read,
    bookmarkCount = bookmarks,
    latestUpload = 0L,
    chapterFetchedAt = 0L,
    lastRead = 0L,
)

/** A library item over [entry] with its own source-manager mock, so no Injekt lookup. */
internal fun libItem(entry: LibraryManga, downloads: Int = 0, local: Boolean = false): LibraryItem = LibraryItem(
    libraryManga = entry,
    downloadCount = downloads,
    unreadCount = entry.unreadCount,
    isLocal = local,
    sourceManager = mockk(relaxed = true),
    badges = LibraryItem.Badges(downloadCount = 0, unreadCount = 0L, isLocal = false, sourceLanguage = ""),
)

/** The library item of a plain manga [id] in [categories]. */
internal fun libItem(id: Long, categories: List<Long> = listOf(1L)): LibraryItem =
    libItem(libEntry(libManga(id), categories))

internal fun libCategory(id: Long, name: String = "Cat $id", flags: Long = 0L): Category =
    Category(id = id, name = name, order = id, flags = flags)

/** Item preferences with every filter off; [filter] fills all the tri-state filters at once. */
internal fun itemPrefs(
    filter: TriState = TriState.DISABLED,
    globalDownloaded: Boolean = false,
    skipOutsideRelease: Boolean = false,
    badges: Boolean = false,
): ItemPreferences = ItemPreferences(
    downloadBadge = badges,
    unreadBadge = badges,
    localBadge = badges,
    languageBadge = badges,
    skipOutsideReleasePeriod = skipOutsideRelease,
    globalFilterDownloaded = globalDownloaded,
    filterDownloaded = filter,
    filterUnread = filter,
    filterStarted = filter,
    filterBookmarked = filter,
    filterCompleted = filter,
    filterIntervalCustom = filter,
    filterLewd = filter,
)

internal fun track(mangaId: Long, trackerId: Long, status: Long = 1L): Track = Track(
    id = mangaId * 100 + trackerId,
    mangaId = mangaId,
    trackerId = trackerId,
    remoteId = 0L,
    libraryId = null,
    title = "T",
    lastChapterRead = 0.0,
    totalChapters = 0L,
    status = status,
    score = 0.0,
    remoteUrl = "",
    startDate = 0L,
    finishDate = 0L,
    private = false,
)

/** A library screen state showing [items] under [category] (all items), optionally with [selection]. */
internal fun stateOf(
    category: Category,
    items: List<LibraryItem>,
    selection: Set<Long> = emptySet(),
): LibraryScreenModel.State = LibraryScreenModel.State(
    selection = selection,
    libraryData = LibraryScreenModel.LibraryData(isInitialized = true, favorites = items),
    groupedFavorites = mapOf(category to items.map { it.id }),
)
