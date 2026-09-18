package tachiyomi.domain.history.interactor

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.CustomMangaInfoScope
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

internal class GetNextChaptersFromTest {

    private val fixture = NextChaptersFixture()

    @BeforeEach
    fun beforeEach() {
        CustomMangaInfoScope.install()
        fixture.withManga(source = 1L, chapters = unsortedChapters)
    }

    @AfterEach
    fun afterEach() {
        CustomMangaInfoScope.restore()
    }

    @Test
    fun noHistoryGivesNothing() = runTest {
        coEvery { fixture.historyRepository.getLastHistory() } returns null

        fixture.interactor.await().shouldBeEmpty()
    }

    @Test
    fun lastHistoryContinuesManga() = runTest {
        coEvery { fixture.historyRepository.getLastHistory() } returns lastRead(chapterId = unreadChapter.id)

        val chapters = fixture.interactor.await(onlyUnread = false)

        chapters shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun lastHistoryUnreadByDefault() = runTest {
        coEvery { fixture.historyRepository.getLastHistory() } returns lastRead(chapterId = readChapter.id)

        fixture.interactor.await() shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun unreadFromKnownChapter() = runTest {
        val chapters = fixture.interactor.await(NEXT_MANGA_ID, lastChapter.id)

        chapters shouldContainExactly listOf(lastChapter)
    }

    @Test
    fun unreadFromUnknownChapter() = runTest {
        val chapters = fixture.interactor.await(NEXT_MANGA_ID, UNKNOWN_CHAPTER_ID)

        chapters shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun allFromUnreadChapter() = runTest {
        val chapters = fixture.interactor.await(NEXT_MANGA_ID, unreadChapter.id, onlyUnread = false)

        chapters shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun allFromReadChapterSkipsIt() = runTest {
        val chapters = fixture.interactor.await(NEXT_MANGA_ID, readChapter.id, onlyUnread = false)

        chapters shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun allFromUnknownChapter() = runTest {
        val chapters = fixture.interactor.await(NEXT_MANGA_ID, UNKNOWN_CHAPTER_ID, onlyUnread = false)

        chapters shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    private companion object {
        const val UNKNOWN_CHAPTER_ID = 99L

        fun lastRead(chapterId: Long): HistoryWithRelations = HistoryWithRelations(
            id = 1L,
            chapterId = chapterId,
            mangaId = NEXT_MANGA_ID,
            ogTitle = "title",
            chapterNumber = 1.0,
            readAt = Date(0L),
            readDuration = 0L,
            coverData = MangaCover(
                mangaId = NEXT_MANGA_ID,
                sourceId = 1L,
                isMangaFavorite = false,
                ogUrl = null,
                lastModified = 0L,
            ),
        )
    }
}
