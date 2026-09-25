package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.StatsScreenState
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.domainTrack
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.base.libraryManga
import eu.kanade.tachiyomi.ui.base.mainReset
import eu.kanade.tachiyomi.ui.base.mainUnconfined
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.history.interactor.GetTotalReadDuration
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetReadMangaNotInLibraryView
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track

internal class StatsScreenModelTest {
    private val downloadManager = mockk<DownloadManager> { every { getDownloadCount() } returns 4 }
    private val getLibraryManga = mockk<GetLibraryManga>()
    private val getTotalReadDuration = mockk<GetTotalReadDuration> { coEvery { await() } returns 60L }
    private val getTracks = mockk<GetTracks>()
    private val preferences = LibraryPreferences(MapPreferenceStore())
    private val tracker = mockk<BaseTracker> {
        every { id } returns 10L
        every { get10PointScore(any()) } answers { firstArg<Track>().score }
    }
    private val otherTracker = mockk<BaseTracker> { every { id } returns 11L }
    private val trackerManager = mockk<TrackerManager> {
        every { loggedInTrackers() } returns listOf(otherTracker, tracker)
    }
    private val getReadNotInLibrary = mockk<GetReadMangaNotInLibraryView>()

    private val completed = libraryManga(1, manga(1, status = 2), listOf(1), totalChapters = 3, readCount = 3)
    private val local = libraryManga(2, manga(2, source = 0), listOf(2), totalChapters = 5)
    private val empty = libraryManga(3)
    private val caughtUp = libraryManga(4, manga(4, status = 1), listOf(1, 2), totalChapters = 2, readCount = 2)

    @BeforeEach
    fun setUp() {
        mainUnconfined()
        startKoin { modules(customInfoModule()) }
        coEvery { getLibraryManga.await() } returns listOf(completed, completed, local, empty, caughtUp)
        coEvery { getReadNotInLibrary.await() } returns listOf(libraryManga(5, totalChapters = 1, readCount = 1))
        coEvery { getTracks.await(any<Long>()) } returns emptyList()
        coEvery { getTracks.await(1L) } returns listOf(
            domainTrack(trackerId = 10L, score = 8.0),
            domainTrack(trackerId = 99L),
        )
        coEvery { getTracks.await(2L) } returns listOf(domainTrack(trackerId = 10L))
    }

    @AfterEach
    fun tearDown() {
        mainReset()
        stopKoin()
    }

    private fun manga(id: Long, status: Long = 0, source: Long = 1) =
        Manga.create().copy(id = id, ogStatus = status, source = source)

    private fun model() = StatsScreenModel(
        downloadManager = downloadManager,
        getLibraryManga = getLibraryManga,
        getTotalReadDuration = getTotalReadDuration,
        getTracks = getTracks,
        preferences = preferences,
        trackerManager = trackerManager,
        getReadMangaNotInLibraryView = getReadNotInLibrary,
    )

    private fun StatsScreenModel.success() = state.value.shouldBeInstanceOf<StatsScreenState.Success>()

    @Test
    fun libraryStatsAreCounted() {
        val state = model().success()
        state.overview.libraryMangaCount shouldBe 4
        state.overview.completedMangaCount shouldBe 1
        state.overview.totalReadDuration shouldBe 60L
        state.titles.startedMangaCount shouldBe 2
        state.titles.localMangaCount shouldBe 1
        state.chapters.totalChapterCount shouldBe 10
        state.chapters.readChapterCount shouldBe 5
        state.chapters.downloadCount shouldBe 4
        state.trackers.trackedTitleCount shouldBe 2
        state.trackers.meanScore shouldBe 8.0
        state.trackers.trackerCount shouldBe 2
    }

    @Test
    fun defaultRestrictionsApply() {
        model().success().titles.globalUpdateItemCount shouldBe 2
    }

    @Test
    fun categoriesLimitUpdates() {
        preferences.updateCategories.set(setOf("1"))
        preferences.updateCategoriesExclude.set(setOf("2"))
        preferences.autoUpdateMangaRestrictions.set(setOf(LibraryPreferences.MANGA_NON_READ))
        model().success().titles.globalUpdateItemCount shouldBe 2
    }

    @Test
    fun notStartedEntriesAreSkipped() {
        preferences.autoUpdateMangaRestrictions.set(setOf(LibraryPreferences.MANGA_NON_READ))
        model().success().titles.globalUpdateItemCount shouldBe 4
    }

    @Test
    fun unrestrictedCountsEverything() {
        preferences.autoUpdateMangaRestrictions.set(emptySet())
        model().success().titles.globalUpdateItemCount shouldBe 5
    }

    @Test
    fun unreadCompletedIsNotCompleted() {
        val unread = libraryManga(6, manga(6, status = 2), totalChapters = 2, readCount = 1)
        coEvery { getLibraryManga.await() } returns listOf(unread)
        model().success().overview.completedMangaCount shouldBe 0
    }

    @Test
    fun nanScoresAreIgnored() {
        every { tracker.get10PointScore(any()) } returns Double.NaN
        model().success().trackers.meanScore.isNaN() shouldBe true
    }

    @Test
    fun allReadAddsNonLibraryEntries() {
        val model = model()
        model.allRead.value shouldBe false
        model.toggleReadManga()
        model.allRead.value shouldBe true
        model.state.await { it is StatsScreenState.Success && it.overview.libraryMangaCount == 5 }
        model.toggleReadManga()
        model.allRead.value shouldBe false
    }

    @Test
    fun defaultsComeFromInjekt() {
        stopKoin()
        startKoin {
            modules(
                customInfoModule(),
                module {
                    single { downloadManager }
                    single { getLibraryManga }
                    single { getTotalReadDuration }
                    single { getTracks }
                    single { preferences }
                    single { trackerManager }
                    single { getReadNotInLibrary }
                },
            )
        }
        StatsScreenModel().success().chapters.downloadCount shouldBe 4
    }
}
