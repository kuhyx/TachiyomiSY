package tachiyomi.domain.history.interactor

import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetNextChaptersTest {

    private val fixture = NextChaptersFixture()

    @Test
    fun missingMangaGivesNothing() = runTest {
        fixture.withoutManga()

        fixture.interactor.await(NEXT_MANGA_ID, onlyUnread = false).shouldBeEmpty()
    }

    @Test
    fun allChaptersInReadingOrder() = runTest {
        fixture.withManga(source = PLAIN_SOURCE_ID, chapters = unsortedChapters)

        val chapters = fixture.interactor.await(NEXT_MANGA_ID, onlyUnread = false)

        chapters shouldContainExactly listOf(readChapter, unreadChapter, lastChapter)
        coVerify(exactly = 0) { fixture.getMergedChaptersByMangaId.await(any(), any(), any()) }
    }

    @Test
    fun unreadChaptersByDefault() = runTest {
        fixture.withManga(source = PLAIN_SOURCE_ID, chapters = unsortedChapters)

        fixture.interactor.await(NEXT_MANGA_ID) shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun mergedMangaReadsMergedSet() = runTest {
        fixture.withManga(source = MERGED_SOURCE_ID, chapters = unsortedChapters)

        val chapters = fixture.interactor.await(NEXT_MANGA_ID, onlyUnread = false)

        chapters shouldContainExactly listOf(readChapter, unreadChapter, lastChapter)
        coVerify(exactly = 0) { fixture.getChaptersByMangaId.await(any(), any()) }
    }

    @Test
    fun mergedMangaFiltersRead() = runTest {
        fixture.withManga(source = MERGED_SOURCE_ID, chapters = unsortedChapters)

        fixture.interactor.await(NEXT_MANGA_ID) shouldContainExactly listOf(unreadChapter, lastChapter)
    }

    @Test
    fun ehGalleryIsItsLastVersion() = runTest {
        fixture.withManga(source = EH_SOURCE_ID, chapters = unsortedChapters)

        fixture.interactor.await(NEXT_MANGA_ID) shouldContainExactly listOf(lastChapter)
    }

    @Test
    fun ehGalleryReadGivesNothing() = runTest {
        val allRead = unsortedChapters.map { it.copy(read = true) }
        fixture.withManga(source = EH_SOURCE_ID, chapters = allRead)

        fixture.interactor.await(NEXT_MANGA_ID).shouldBeEmpty()
    }

    @Test
    fun exhGalleryWithoutChapters() = runTest {
        fixture.withManga(source = EXH_SOURCE_ID, chapters = emptyList())

        fixture.interactor.await(NEXT_MANGA_ID).shouldBeEmpty()
    }

    @Test
    fun ehGalleryAllChapters() = runTest {
        fixture.withManga(source = EXH_SOURCE_ID, chapters = unsortedChapters)

        val chapters = fixture.interactor.await(NEXT_MANGA_ID, onlyUnread = false)

        chapters shouldContainExactly listOf(readChapter, unreadChapter, lastChapter)
    }

    private companion object {
        const val PLAIN_SOURCE_ID = 1L
    }
}
