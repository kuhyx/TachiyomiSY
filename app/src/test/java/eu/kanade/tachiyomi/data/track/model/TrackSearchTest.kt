package eu.kanade.tachiyomi.data.track.model

import eu.kanade.tachiyomi.data.database.models.Track
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class TrackSearchTest {

    private fun search(mangaId: Long = 1L, trackerId: Long = 2L, remoteId: Long = 3L): TrackSearch =
        TrackSearch.create(trackerId).apply {
            this.mangaId = mangaId
            this.remoteId = remoteId
        }

    @Test
    fun createSetsTheTrackerOnly() {
        val created = TrackSearch.create(7L)
        created.trackerId shouldBe 7L
        created.id shouldBe null
        created.mangaId shouldBe 0L
        created.remoteId shouldBe 0L
        created.libraryId shouldBe null
        created.title shouldBe ""
        created.lastChapterRead shouldBe 0.0
        created.totalChapters shouldBe 0L
        created.score shouldBe -1.0
        created.status shouldBe 0L
        created.startedReadingDate shouldBe 0L
        created.finishedReadingDate shouldBe 0L
        created.private shouldBe false
        created.trackingUrl shouldBe ""
        created.authors shouldBe emptyList()
        created.artists shouldBe emptyList()
        created.coverUrl shouldBe ""
        created.summary shouldBe ""
        created.publishingStatus shouldBe ""
        created.publishingType shouldBe ""
        created.startDate shouldBe ""
    }

    @Test
    fun everyFieldIsWritable() {
        val item = TrackSearch.create(1L).apply {
            id = 9L
            libraryId = 8L
            title = "T"
            lastChapterRead = 2.5
            totalChapters = 10L
            score = 4.0
            status = 3L
            startedReadingDate = 100L
            finishedReadingDate = 200L
            private = true
            trackingUrl = "https://x"
            authors = listOf("a")
            artists = listOf("b")
            coverUrl = "c"
            summary = "s"
            publishingStatus = "ongoing"
            publishingType = "Manga"
            startDate = "2020-01-01"
        }
        item.id shouldBe 9L
        item.libraryId shouldBe 8L
        item.title shouldBe "T"
        item.lastChapterRead shouldBe 2.5
        item.totalChapters shouldBe 10L
        item.score shouldBe 4.0
        item.status shouldBe 3L
        item.startedReadingDate shouldBe 100L
        item.finishedReadingDate shouldBe 200L
        item.private shouldBe true
        item.trackingUrl shouldBe "https://x"
        item.authors shouldBe listOf("a")
        item.artists shouldBe listOf("b")
        item.coverUrl shouldBe "c"
        item.summary shouldBe "s"
        item.publishingStatus shouldBe "ongoing"
        item.publishingType shouldBe "Manga"
        item.startDate shouldBe "2020-01-01"
    }

    @Test
    fun equalsComparesIdentityTriple() {
        val item = search()
        (item == item) shouldBe true
        val absent: TrackSearch? = null
        (item == absent) shouldBe false
        (item == search()) shouldBe true
        (item == search(mangaId = 5L)) shouldBe false
        (item == search(trackerId = 5L)) shouldBe false
        (item == search(remoteId = 5L)) shouldBe false
        item.equals(Track.create(2L)) shouldBe false
    }

    @Test
    fun hashCodeFollowsEquals() {
        search().hashCode() shouldBe search().hashCode()
        search().hashCode() shouldNotBe search(remoteId = 4L).hashCode()
    }
}
