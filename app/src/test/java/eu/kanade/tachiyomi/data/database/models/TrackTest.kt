package eu.kanade.tachiyomi.data.database.models

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private fun source(): Track = Track.create(serviceId = 3L).also {
    it.lastChapterRead = 12.5
    it.score = 8.0
    it.status = 2L
    it.startedReadingDate = 100L
    it.finishedReadingDate = 200L
    it.private = true
}

internal class TrackTest {

    @Test
    fun createSetsTheTracker() {
        val track = Track.create(serviceId = 7L)
        track.trackerId shouldBe 7L
        track.id.shouldBeNull()
        track.mangaId shouldBe 0L
        track.remoteId shouldBe 0L
        track.libraryId.shouldBeNull()
        track.title shouldBe ""
        track.totalChapters shouldBe 0L
        track.trackingUrl shouldBe ""
    }

    @Test
    fun copyPersonalCopiesPrivate() {
        val target = Track.create(serviceId = 3L)
        target.copyPersonalFrom(source())
        target.lastChapterRead shouldBe 12.5
        target.score shouldBe 8.0
        target.status shouldBe 2L
        target.startedReadingDate shouldBe 100L
        target.finishedReadingDate shouldBe 200L
        target.private shouldBe true
    }

    @Test
    fun copyPersonalKeepsPrivate() {
        val target = Track.create(serviceId = 3L)
        target.copyPersonalFrom(other = source(), copyRemotePrivate = false)
        target.lastChapterRead shouldBe 12.5
        target.private shouldBe false
    }

    @Test
    fun implFieldsAreMutable() {
        val track = TrackImpl()
        track.id = 1L
        track.mangaId = 2L
        track.remoteId = 3L
        track.libraryId = 4L
        track.title = "t"
        track.totalChapters = 5L
        track.trackingUrl = "u"
        listOf(track.id, track.mangaId, track.remoteId, track.libraryId) shouldBe listOf(1L, 2L, 3L, 4L)
        listOf(track.title, track.trackingUrl) shouldBe listOf("t", "u")
        track.totalChapters shouldBe 5L
    }
}
