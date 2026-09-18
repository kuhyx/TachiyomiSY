package exh.metadata.metadata.base

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private class TrackerIds : TrackerIdMetadata {
    override var anilistId: String? = null
    override var kitsuId: String? = null
    override var myAnimeListId: String? = null
    override var mangaUpdatesId: String? = null
    override var animePlanetId: String? = null
}

internal class TrackerIdMetadataTest {
    @Test
    fun idsStartNull() {
        val ids: TrackerIdMetadata = TrackerIds()
        listOf(ids.anilistId, ids.kitsuId, ids.myAnimeListId, ids.mangaUpdatesId, ids.animePlanetId)
            .all { it == null } shouldBe true
    }

    @Test
    fun idsAreWritableThroughInterface() {
        val ids: TrackerIdMetadata = TrackerIds()
        ids.anilistId = "al"
        ids.kitsuId = "ki"
        ids.myAnimeListId = "mal"
        ids.mangaUpdatesId = "mu"
        ids.animePlanetId = "ap"
        listOf(ids.anilistId, ids.kitsuId, ids.myAnimeListId, ids.mangaUpdatesId, ids.animePlanetId) shouldBe
            listOf("al", "ki", "mal", "mu", "ap")
    }
}
