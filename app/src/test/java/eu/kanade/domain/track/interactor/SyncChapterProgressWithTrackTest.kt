package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.domainTrack
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.track.interactor.InsertTrack
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

/** A stored chapter numbered [number] of manga 9. */
internal fun numberedChapter(number: Double, read: Boolean = false): Chapter =
    Chapter.create().copy(id = (number * 10).toLong(), mangaId = 9, chapterNumber = number, read = read)

internal class SyncChapterProgressWithTrackTest {

    private val updateChapter = mockk<UpdateChapter>()
    private val insertTrack = mockk<InsertTrack>()
    private val getChaptersByMangaId = mockk<GetChaptersByMangaId>()
    private val interactor = SyncChapterProgressWithTrack(updateChapter, insertTrack, getChaptersByMangaId)

    @Test
    fun ignoresPlainTrackers() = runTest {
        interactor.await(9, domainTrack(), mockk<Tracker>())
        coVerify(exactly = 0) { getChaptersByMangaId.await(any()) }
    }

    @Test
    fun marksRemoteProgressReadLocally() = runTest {
        val tracker = mockk<EnhancedTracker>(moreInterfaces = arrayOf(Tracker::class))
        coEvery { (tracker as Tracker).update(any(), any()) } answers { firstArg() }
        coEvery { getChaptersByMangaId.await(9) } returns listOf(
            numberedChapter(3.0),
            numberedChapter(1.0, read = true),
            numberedChapter(2.0, read = true),
            numberedChapter(-1.0),
            numberedChapter(4.0),
            numberedChapter(5.0, read = true),
        )
        val updates = slot<List<ChapterUpdate>>()
        coEvery { updateChapter.awaitAll(capture(updates)) } returns Unit
        coEvery { insertTrack.await(any()) } returns Unit
        interactor.await(9, domainTrack(lastChapterRead = 3.0), tracker as Tracker)
        updates.captured.map { it.id } shouldBe listOf(30L)
        updates.captured.single().read shouldBe true
        coVerify(exactly = 1) { insertTrack.await(domainTrack(lastChapterRead = 3.0)) }
    }

    @Test
    fun pushesLocalProgressWhenAhead() = runTest {
        val tracker = mockk<EnhancedTracker>(moreInterfaces = arrayOf(Tracker::class))
        val pushed = slot<DbTrack>()
        coEvery { (tracker as Tracker).update(capture(pushed), any()) } answers { firstArg() }
        coEvery { getChaptersByMangaId.await(9) } returns listOf(
            numberedChapter(1.0, read = true),
            numberedChapter(2.0, read = true),
            numberedChapter(3.0),
        )
        coEvery { updateChapter.awaitAll(emptyList()) } returns Unit
        coEvery { insertTrack.await(any()) } returns Unit
        interactor.await(9, domainTrack(lastChapterRead = 1.0), tracker as Tracker)
        pushed.captured.lastChapterRead shouldBe 2.0
        coVerify(exactly = 1) { insertTrack.await(domainTrack(lastChapterRead = 2.0)) }
    }

    @Test
    fun noLocalProgressFallsBackToZero() = runTest {
        val tracker = mockk<EnhancedTracker>(moreInterfaces = arrayOf(Tracker::class))
        coEvery { (tracker as Tracker).update(any(), any()) } answers { firstArg() }
        coEvery { getChaptersByMangaId.await(9) } returns emptyList()
        coEvery { updateChapter.awaitAll(emptyList()) } returns Unit
        coEvery { insertTrack.await(any()) } returns Unit
        interactor.await(9, domainTrack(lastChapterRead = 0.0), tracker as Tracker)
        coVerify(exactly = 1) { insertTrack.await(domainTrack(lastChapterRead = 0.0)) }
    }

    @Test
    fun logsAFailedPush() = runTest {
        val tracker = mockk<EnhancedTracker>(moreInterfaces = arrayOf(Tracker::class))
        coEvery { (tracker as Tracker).update(any(), any()) } throws IllegalStateException("offline")
        coEvery { getChaptersByMangaId.await(9) } returns emptyList()
        interactor.await(9, domainTrack(), tracker as Tracker)
        coVerify(exactly = 0) { insertTrack.await(any()) }
    }
}
