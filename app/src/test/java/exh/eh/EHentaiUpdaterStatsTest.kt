package exh.eh

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class EHentaiUpdaterStatsTest {
    @Test
    fun statsRoundTripThroughJson() {
        val stats = EHentaiUpdaterStats(startTime = 5L, possibleUpdates = 3, updateCount = 2)
        val json = Json.encodeToString(stats)
        json shouldBe """{"startTime":5,"possibleUpdates":3,"updateCount":2}"""
        Json.decodeFromString<EHentaiUpdaterStats>(json) shouldBe stats
        stats.copy(updateCount = 1).updateCount shouldBe 1
        stats.hashCode() shouldBe stats.copy().hashCode()
        stats.toString() shouldBe "EHentaiUpdaterStats(startTime=5, possibleUpdates=3, updateCount=2)"
    }

    @Test
    fun galleryNotUpdatedKeepsCause() {
        val cause = IllegalStateException("x")
        val network = GalleryNotUpdatedException(true, cause)
        network.network.shouldBeTrue()
        network.cause shouldBeSameInstanceAs cause
        GalleryNotUpdatedException(false, cause).network.shouldBeFalse()
    }

    @Test
    fun updateEntryHoldsItsParts() {
        val manga = ehManga(1)
        val entry = UpdateEntry(manga, exh.metadata.metadata.EHentaiSearchMetadata(), null)
        entry.manga shouldBeSameInstanceAs manga
        entry.rootChapter shouldBe null
        EHentaiUpdateWorkerConstants.UPDATES_PER_ITERATION shouldBe 50
        EHentaiUpdateWorkerConstants.GALLERY_AGE_TIME shouldBe 365L * 24 * 60 * 60 * 1000
    }

    @Test
    fun allTagsAreUniqueAndNamespaced() {
        val all = EHTags.getAllTags()
        all.shouldBeUnique()
        all.size shouldBe 29_009
        val namespaces = EHTags.getNamespaces()
        namespaces.size shouldBe 12
        namespaces shouldContainExactly listOf(
            "reclass", "language", "parody", "character", "group", "artist",
            "cosplayer", "male", "female", "mixed", "location", "other",
        )
    }
}
