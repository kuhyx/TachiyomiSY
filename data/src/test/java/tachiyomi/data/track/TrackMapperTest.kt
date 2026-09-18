package tachiyomi.data.track

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.Manga_sync
import tachiyomi.domain.track.model.Track

internal class TrackMapperTest {
    @Test
    fun mapsEveryColumn() {
        val row = Manga_sync(
            _id = 1L, manga_id = 2L, sync_id = 3L, remote_id = 4L, library_id = 5L, title = "Title",
            last_chapter_read = 6.5, total_chapters = 7L, status = 8L, score = 9.5, remote_url = "https://t/4",
            start_date = 10L, finish_date = 11L, private_ = true,
        )
        TrackMapper.mapTrack(row) shouldBe Track(
            id = 1L, mangaId = 2L, trackerId = 3L, remoteId = 4L, libraryId = 5L, title = "Title",
            lastChapterRead = 6.5, totalChapters = 7L, status = 8L, score = 9.5, remoteUrl = "https://t/4",
            startDate = 10L, finishDate = 11L, private = true,
        )
    }

    @Test
    fun keepsNullLibraryId() {
        val row = Manga_sync(
            _id = 1L, manga_id = 2L, sync_id = 3L, remote_id = 4L, library_id = null, title = "Title",
            last_chapter_read = 0.0, total_chapters = 0L, status = 0L, score = 0.0, remote_url = "",
            start_date = 0L, finish_date = 0L, private_ = false,
        )
        val track = TrackMapper.mapTrack(row)
        track.libraryId shouldBe null
        track.private shouldBe false
    }
}
