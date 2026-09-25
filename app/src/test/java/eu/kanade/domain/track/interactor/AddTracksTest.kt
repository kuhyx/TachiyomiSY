package eu.kanade.domain.track.interactor

import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.track.model.domainTrack
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.Source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import java.util.Date

internal class AddTracksTest {

    private val insertTrack = mockk<InsertTrack>()
    private val sync = mockk<SyncChapterProgressWithTrack>()
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val trackerManager = mockk<TrackerManager>()
    private val getHistory = mockk<GetHistory>()
    private val interactor = AddTracks(insertTrack, sync, getChaptersByMangaId, trackerManager)
    private val tracker = mockk<Tracker>()

    private val logged = captureLogcat()

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { getHistory } }) }
        coEvery { insertTrack.await(any()) } returns Unit
        coEvery { sync.await(any(), any(), any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
        releaseLogcat()
    }

    @Test
    fun bindsWithoutLocalProgress() = runTest {
        coEvery { getChaptersByMangaId.await(9) } returns listOf(numberedChapter(1.0))
        val item = domainTrack(id = 5, startDate = 0).toDbTrack()
        coEvery { tracker.bind(item, false) } returns item
        interactor.bind(tracker, item, 9)
        coVerify(exactly = 1) { insertTrack.await(domainTrack(id = 5, startDate = 0)) }
        coVerify(exactly = 0) { tracker.setRemoteLastChapterRead(any(), any()) }
        coVerify(exactly = 1) { sync.await(9, domainTrack(id = 5, startDate = 0), tracker) }
    }

    @Test
    fun pushesProgressAndStartDate() = runTest {
        coEvery { getChaptersByMangaId.await(9) } returns listOf(
            numberedChapter(2.0, read = true),
            numberedChapter(1.0, read = true),
            numberedChapter(3.0),
        )
        coEvery { getHistory.await(9) } returns listOf(
            History(id = 1, chapterId = 10, readAt = Date(5_000), readDuration = 0),
            History(id = 2, chapterId = 20, readAt = Date(1_000), readDuration = 0),
        )
        val item = domainTrack(lastChapterRead = 1.0, startDate = 0).toDbTrack()
        coEvery { tracker.bind(item, true) } returns item
        coEvery { tracker.setRemoteLastChapterRead(any(), 2) } returns Unit
        val startDate = slot<Long>()
        coEvery { tracker.setRemoteStartDate(any(), capture(startDate)) } returns Unit
        interactor.bind(tracker, item, 9)
        val bound = slot<Track>()
        coVerify(exactly = 1) { sync.await(9, capture(bound), tracker) }
        bound.captured.lastChapterRead shouldBe 2.0
        bound.captured.startDate shouldBe startDate.captured
    }

    @Test
    fun keepsRemoteProgressWhenAhead() = runTest {
        coEvery { getChaptersByMangaId.await(9) } returns listOf(numberedChapter(1.0, read = true))
        coEvery { getHistory.await(9) } returns emptyList()
        val item = domainTrack(lastChapterRead = 4.0, startDate = 0).toDbTrack()
        coEvery { tracker.bind(item, true) } returns item
        interactor.bind(tracker, item, 9)
        coVerify(exactly = 0) { tracker.setRemoteLastChapterRead(any(), any()) }
        coVerify(exactly = 0) { tracker.setRemoteStartDate(any(), any()) }
    }

    @Test
    fun keepsAnExistingStartDate() = runTest {
        coEvery { getChaptersByMangaId.await(9) } returns listOf(numberedChapter(-1.0, read = true))
        val item = domainTrack(lastChapterRead = 0.0, startDate = 100).toDbTrack()
        coEvery { tracker.bind(item, true) } returns item
        interactor.bind(tracker, item, 9)
        coVerify(exactly = 0) { tracker.setRemoteLastChapterRead(any(), any()) }
        coVerify(exactly = 0) { getHistory.await(any()) }
    }

    @Test
    fun bindsAcceptingEnhanced() = runTest {
        val source = mockk<Source>()
        val manga = Manga.create().copy(id = 9, ogTitle = "Title")
        val enhanced = mockk<BaseTracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
        val declining = mockk<BaseTracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
        val unmatched = mockk<BaseTracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
        val failing = mockk<BaseTracker>(moreInterfaces = arrayOf(EnhancedTracker::class))
        every { trackerManager.loggedInTrackers() } returns listOf(mockk(), enhanced, declining, unmatched, failing)
        every { (declining as EnhancedTracker).accept(source) } returns false
        every { (enhanced as EnhancedTracker).accept(source) } returns true
        every { (unmatched as EnhancedTracker).accept(source) } returns true
        every { (failing as EnhancedTracker).accept(source) } returns true
        val matched = TrackSearch.create(1).apply { remoteId = 3 }
        coEvery { (enhanced as EnhancedTracker).match(manga) } returns matched
        coEvery { (unmatched as EnhancedTracker).match(manga) } returns null
        coEvery { (failing as EnhancedTracker).match(manga) } throws IllegalStateException("no")
        coEvery { enhanced.bind(matched, false) } returns matched
        interactor.bindEnhancedTrackers(manga, source)
        matched.mangaId shouldBe 9L
        coVerify(exactly = 1) { insertTrack.await(matched.toDomainTrack(idRequired = false)!!) }
        coVerify(exactly = 1) { sync.await(9, matched.toDomainTrack(idRequired = false)!!, enhanced) }
        logged.single() shouldContain "Could not match manga: Title"
    }

    @Test
    fun readChaptersNeedNotStartAtOne() = runTest {
        coEvery { getChaptersByMangaId.await(9) } returns listOf(
            numberedChapter(1.0),
            numberedChapter(2.0, read = true),
        )
        val item = domainTrack(lastChapterRead = 0.0, startDate = 100).toDbTrack()
        coEvery { tracker.bind(item, true) } returns item
        interactor.bind(tracker, item, 9)
        coVerify(exactly = 0) { tracker.setRemoteLastChapterRead(any(), any()) }
    }
}
