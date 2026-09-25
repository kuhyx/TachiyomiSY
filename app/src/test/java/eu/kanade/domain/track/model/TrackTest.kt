package eu.kanade.domain.track.model

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.track.model.Track
import eu.kanade.tachiyomi.data.database.models.Track as DbTrack

/** A fully populated domain track for [trackerId]. */
internal fun domainTrack(
    id: Long = 5,
    trackerId: Long = 2,
    lastChapterRead: Double = 3.0,
    status: Long = 1,
    startDate: Long = 100,
): Track = Track(
    id = id,
    mangaId = 9,
    trackerId = trackerId,
    remoteId = 77,
    libraryId = 11,
    title = "Title",
    lastChapterRead = lastChapterRead,
    totalChapters = 20,
    status = status,
    score = 8.5,
    remoteUrl = "https://tracker/77",
    startDate = startDate,
    finishDate = 200,
    private = true,
)

internal class TrackTest {

    @Test
    fun copiesPersonalFields() {
        val base = domainTrack()
        val other = domainTrack(lastChapterRead = 12.0, status = 3, startDate = 500).copy(
            score = 1.0,
            finishDate = 600,
            private = false,
            title = "Other",
        )
        val copied = base.copyPersonalFrom(other)
        copied.lastChapterRead shouldBe 12.0
        copied.score shouldBe 1.0
        copied.status shouldBe 3
        copied.startDate shouldBe 500
        copied.finishDate shouldBe 600
        copied.private shouldBe false
        copied.title shouldBe "Title"
    }

    @Test
    fun roundTripsThroughTheDbModel() {
        val track = domainTrack()
        val db = track.toDbTrack()
        db.id shouldBe 5L
        db.mangaId shouldBe 9L
        db.trackerId shouldBe 2L
        db.remoteId shouldBe 77L
        db.libraryId shouldBe 11L
        db.trackingUrl shouldBe "https://tracker/77"
        db.startedReadingDate shouldBe 100L
        db.finishedReadingDate shouldBe 200L
        db.private shouldBe true
        db.toDomainTrack() shouldBe track
        db.toDomainTrack(idRequired = false) shouldBe track
    }

    @Test
    fun anUnsavedDbTrackNeedsNoId() {
        val db = DbTrack.create(2).apply { mangaId = 9 }
        db.toDomainTrack().shouldBeNull()
        db.toDomainTrack(idRequired = false)!!.id shouldBe -1L
    }
}
