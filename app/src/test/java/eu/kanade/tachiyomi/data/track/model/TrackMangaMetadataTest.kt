package eu.kanade.tachiyomi.data.track.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class TrackMangaMetadataTest {

    @Test
    fun defaultsAreAllNull() {
        val empty = TrackMangaMetadata()
        empty.remoteId shouldBe null
        empty.title shouldBe null
        empty.thumbnailUrl shouldBe null
        empty.description shouldBe null
        empty.authors shouldBe null
        empty.artists shouldBe null
    }

    @Test
    fun dataClassContract() {
        val full = TrackMangaMetadata(
            remoteId = 1L,
            title = "t",
            thumbnailUrl = "u",
            description = "d",
            authors = "a",
            artists = "b",
        )
        full.copy() shouldBe full
        full.copy(title = "other") shouldNotBe full
        full.hashCode() shouldBe full.copy().hashCode()
        full.toString() shouldBe
            "TrackMangaMetadata(remoteId=1, title=t, thumbnailUrl=u, description=d, authors=a, artists=b)"
        full.component2() shouldBe "t"
    }
}
